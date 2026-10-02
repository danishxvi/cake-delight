/* ===========================================================
   Cake Delight - frontend logic
   Plain JavaScript. Every request goes through the API Gateway;
   the page never calls a microservice directly.
   =========================================================== */
'use strict';

// The gateway is published on port 30080 of the host serving this page.
const API_BASE_URL = `${window.location.protocol}//${window.location.hostname}:30080/api`;
const ORDERS_STORAGE_KEY = 'cakeDelightSessionOrders';
const FILTER_DEBOUNCE_MS = 300;
const NOTIFICATION_POLL_ATTEMPTS = 10;
const NOTIFICATION_POLL_INTERVAL_MS = 1000;

const state = {
    cakes: [],             // full catalog, used for the category and rating selectors
    basketOrderId: null,   // id of the PENDING order that backs the basket
    basket: null,          // last basket (order) returned by order-service
    sessionOrders: [],     // orders checked out in this browser session
    ratingScore: 0,
};

let alertTimer;
let alertHideTimer;
let filterTimer;

document.addEventListener('DOMContentLoaded', () => {
    bindEvents();
    loadSessionOrders();
    renderOrdersDrawer();
    loadInitialCatalog();
});

/* ---------- API access ---------- */

class ApiError extends Error {
    constructor(message, status) {
        super(message);
        this.status = status;
    }
}

/**
 * Calls the gateway and returns the parsed JSON body. Failed responses are
 * thrown as ApiError carrying the message from the service's error body.
 */
async function api(path, { method = 'GET', body } = {}) {
    let response;
    try {
        response = await fetch(`${API_BASE_URL}${path}`, {
            method,
            headers: body === undefined ? undefined : { 'Content-Type': 'application/json' },
            body: body === undefined ? undefined : JSON.stringify(body),
        });
    } catch (networkError) {
        throw new ApiError('Cannot reach the API Gateway. Is the backend running?', 0);
    }
    const payload = await response.json().catch(() => null);
    if (!response.ok) {
        const message = payload && payload.message ? payload.message : `Request failed (HTTP ${response.status})`;
        throw new ApiError(message, response.status);
    }
    return payload;
}

/* ---------- Helpers ---------- */

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function formatPrice(amount) {
    return Number(amount || 0).toFixed(2);
}

function byId(id) {
    return document.getElementById(id);
}

/* ---------- Event wiring ---------- */

function bindEvents() {
    ['filter-name', 'filter-min-price', 'filter-max-price'].forEach(id =>
        byId(id).addEventListener('input', scheduleFilter));
    byId('filter-category').addEventListener('change', applyFilters);
    byId('reset-filters').addEventListener('click', resetFilters);

    byId('catalog-container').addEventListener('click', event => {
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        const cakeId = Number(button.dataset.cakeId);
        if (button.dataset.action === 'add') addToBasket(cakeId);
        if (button.dataset.action === 'rating') viewAverageRating(cakeId);
    });

    byId('basket-items').addEventListener('click', event => {
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        const itemId = Number(button.dataset.itemId);
        if (button.dataset.action === 'increase') changeQuantity(itemId, 1);
        if (button.dataset.action === 'decrease') changeQuantity(itemId, -1);
        if (button.dataset.action === 'remove') removeFromBasket(itemId);
    });

    byId('checkout').addEventListener('click', checkoutOrder);
    byId('reset-basket').addEventListener('click', resetBasket);

    byId('open-orders').addEventListener('click', openOrdersPanel);
    byId('close-orders').addEventListener('click', closeOrdersPanel);
    byId('orders-overlay').addEventListener('click', closeOrdersPanel);
    document.addEventListener('keydown', event => {
        if (event.key === 'Escape') closeOrdersPanel();
    });

    document.querySelectorAll('.star').forEach(star => {
        star.addEventListener('click', () => setRating(Number(star.dataset.value)));
        star.addEventListener('keydown', event => {
            if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                setRating(Number(star.dataset.value));
            }
        });
    });
    byId('submit-rating').addEventListener('click', submitRating);
}

/* ---------- Catalog ---------- */

async function loadInitialCatalog() {
    try {
        state.cakes = await api('/catalog');
        populateSelectors();
        renderCatalog(state.cakes);
    } catch (error) {
        showAlert(error.message, false);
        byId('catalog-container').innerHTML =
            '<p class="muted-text">Cannot load the catalog right now. Check that the backend is running.</p>';
    }
}

function populateSelectors() {
    const categories = [...new Set(state.cakes.map(cake => cake.category))].sort();
    byId('filter-category').innerHTML = '<option value="">All categories</option>' +
        categories.map(category => `<option value="${escapeHtml(category)}">${escapeHtml(category)}</option>`).join('');

    byId('rate-cake').innerHTML = '<option value="">Choose a cake...</option>' +
        state.cakes.map(cake => `<option value="${cake.id}">${escapeHtml(cake.name)}</option>`).join('');
}

function renderCatalog(cakes) {
    const container = byId('catalog-container');
    if (cakes.length === 0) {
        container.innerHTML = '<p class="muted-text">No cakes match these filters.</p>';
        return;
    }
    container.innerHTML = cakes.map((cake, index) => {
        const available = cake.availability !== false;
        return `
            <div class="cake-card${available ? '' : ' unavailable'}" style="animation-delay: ${index * 0.05}s">
                <div>
                    <h3>${escapeHtml(cake.name)}</h3>
                    <p>${escapeHtml(cake.description)}</p>
                    <p class="cake-category">${escapeHtml(cake.category)}</p>
                </div>
                <div class="cake-card-footer">
                    <div class="price">$${formatPrice(cake.price)}</div>
                    <button type="button" data-action="add" data-cake-id="${cake.id}" ${available ? '' : 'disabled'}>
                        ${available ? 'Add to Basket' : 'Currently Unavailable'}
                    </button>
                    <button type="button" class="btn-outline" data-action="rating" data-cake-id="${cake.id}">View Rating</button>
                </div>
            </div>`;
    }).join('');
}

function scheduleFilter() {
    clearTimeout(filterTimer);
    filterTimer = setTimeout(applyFilters, FILTER_DEBOUNCE_MS);
}

/** Sends every active filter to the catalog service in a single query. */
async function applyFilters() {
    const params = new URLSearchParams();
    const name = byId('filter-name').value.trim();
    const category = byId('filter-category').value;
    const minPrice = byId('filter-min-price').value;
    const maxPrice = byId('filter-max-price').value;

    if (minPrice !== '' && maxPrice !== '' && Number(minPrice) > Number(maxPrice)) {
        showAlert('Min price cannot be greater than max price.', false);
        return;
    }
    if (name) params.set('name', name);
    if (category) params.set('category', category);
    if (minPrice !== '') params.set('minPrice', minPrice);
    if (maxPrice !== '') params.set('maxPrice', maxPrice);

    try {
        const query = params.toString();
        renderCatalog(await api(`/catalog${query ? `?${query}` : ''}`));
    } catch (error) {
        showAlert(error.message, false);
    }
}

function resetFilters() {
    ['filter-name', 'filter-category', 'filter-min-price', 'filter-max-price'].forEach(id => {
        byId(id).value = '';
    });
    applyFilters();
}

/* ---------- Basket (stored server-side by order-service) ---------- */

async function ensureBasket() {
    if (!state.basketOrderId) {
        const order = await api('/orders', { method: 'POST' });
        state.basketOrderId = order.id;
    }
    return state.basketOrderId;
}

/**
 * Applies a basket change and re-renders the basket from the server's
 * response. On a 404 or 409 the basket is re-read from the server, because
 * it may have been checked out or deleted elsewhere.
 */
async function updateBasket(action) {
    try {
        syncBasket(await action());
        return true;
    } catch (error) {
        showAlert(error.message, false);
        if (error.status === 404 || error.status === 409) {
            await refreshBasket();
        }
        return false;
    }
}

async function refreshBasket() {
    if (!state.basketOrderId) return;
    try {
        const order = await api(`/orders/${state.basketOrderId}`);
        if (order.status === 'PENDING') {
            syncBasket(order);
        } else {
            clearBasket();
        }
    } catch (error) {
        clearBasket();
    }
}

async function addToBasket(cakeId) {
    const cake = state.cakes.find(c => c.id === cakeId);
    const added = await updateBasket(async () => {
        const orderId = await ensureBasket();
        return api(`/orders/${orderId}/items`, { method: 'POST', body: { cakeId, quantity: 1 } });
    });
    if (added) showAlert(`Added ${cake ? cake.name : 'cake'} to basket.`, true);
}

async function changeQuantity(itemId, delta) {
    const item = state.basket && state.basket.items.find(i => i.id === itemId);
    if (!item) return;
    if (item.quantity + delta < 1) {
        await removeFromBasket(itemId);
        return;
    }
    await updateBasket(() => api(`/orders/${state.basketOrderId}/items/${itemId}`,
        { method: 'PUT', body: { quantity: item.quantity + delta } }));
}

async function removeFromBasket(itemId) {
    const element = document.querySelector(`.basket-item[data-item-id="${itemId}"]`);
    if (element) {
        element.classList.add('removing');
        await new Promise(resolve => setTimeout(resolve, 300));
    }
    const removed = await updateBasket(() => api(`/orders/${state.basketOrderId}/items/${itemId}`, { method: 'DELETE' }));
    if (removed) showAlert('Item removed from basket.', true);
}

function syncBasket(order) {
    state.basketOrderId = order.id;
    state.basket = order;
    renderBasket();
}

function clearBasket() {
    state.basketOrderId = null;
    state.basket = null;
    renderBasket();
}

function renderBasket() {
    const container = byId('basket-items');
    const items = state.basket ? state.basket.items : [];

    if (items.length === 0) {
        container.innerHTML = '<p class="muted-text empty-text">Your basket is empty.</p>';
        byId('basket-total').innerText = '0.00';
        return;
    }
    container.innerHTML = items.map(item => `
        <div class="basket-item" data-item-id="${item.id}">
            <span class="basket-item-name">${escapeHtml(item.cakeName)}</span>
            <span class="qty-controls">
                <button type="button" data-action="decrease" data-item-id="${item.id}" aria-label="Decrease quantity">-</button>
                <span>${item.quantity}</span>
                <button type="button" data-action="increase" data-item-id="${item.id}" aria-label="Increase quantity">+</button>
            </span>
            <span class="basket-item-total">$${formatPrice(item.lineTotal)}</span>
            <button type="button" class="btn-remove" data-action="remove" data-item-id="${item.id}" aria-label="Remove item">&times;</button>
        </div>`).join('');
    byId('basket-total').innerText = formatPrice(state.basket.totalAmount);
}

function animateBasketClear(callback) {
    const items = document.querySelectorAll('.basket-item');
    items.forEach(item => item.classList.add('removing'));
    setTimeout(callback, items.length ? 300 : 0);
}

async function checkoutOrder() {
    if (!state.basket || state.basket.items.length === 0) {
        showAlert('Your basket is empty.', false);
        return;
    }
    const button = byId('checkout');
    button.disabled = true;
    try {
        const order = await api(`/orders/${state.basketOrderId}/checkout`, { method: 'POST' });
        recordSessionOrder(order);
        showAlert(`Order #${order.id} placed successfully.`, true);
        animateBasketClear(clearBasket);
        pollOrderConfirmation(order.id);
    } catch (error) {
        showAlert(`Checkout failed: ${error.message}`, false);
    } finally {
        button.disabled = false;
    }
}

function resetBasket() {
    if (!state.basket || state.basket.items.length === 0) {
        showAlert('Your basket is already empty.', false);
        return;
    }
    // The server-side basket is simply abandoned and stays PENDING, like an
    // abandoned shopping cart. A new one is created on the next "Add".
    animateBasketClear(clearBasket);
    showAlert('Basket has been reset.', true);
}

/* ---------- Order confirmation (in-app notification) ---------- */

/**
 * The confirmation is created asynchronously by notification-service after it
 * consumes the order-completion event, so poll for it briefly.
 */
async function pollOrderConfirmation(orderId) {
    for (let attempt = 0; attempt < NOTIFICATION_POLL_ATTEMPTS; attempt++) {
        await new Promise(resolve => setTimeout(resolve, NOTIFICATION_POLL_INTERVAL_MS));
        const sent = await fetchConfirmation(orderId);
        if (sent) {
            showAlert(sent.message, true);
            return;
        }
    }
    updateSessionOrder(orderId, { confirmation: { status: 'PENDING' } });
}

/** Returns the SENT confirmation for an order, or null if there is none yet. */
async function fetchConfirmation(orderId) {
    try {
        const notifications = await api(`/notifications/order/${orderId}`);
        const sent = notifications.find(n => n.status === 'SENT');
        if (sent) {
            updateSessionOrder(orderId, { confirmation: { status: 'SENT', message: sent.message } });
        }
        return sent || null;
    } catch (error) {
        return null;
    }
}

function refreshPendingConfirmations() {
    state.sessionOrders
        .filter(order => order.confirmation && order.confirmation.status === 'PENDING')
        .forEach(order => fetchConfirmation(order.id));
}

/* ---------- My Orders (session history) ---------- */
// Kept in sessionStorage because the application has no user accounts:
// "my orders" means the orders placed in this browser tab.

function loadSessionOrders() {
    try {
        const stored = sessionStorage.getItem(ORDERS_STORAGE_KEY);
        state.sessionOrders = stored ? JSON.parse(stored) : [];
    } catch (error) {
        state.sessionOrders = [];
    }
}

function saveSessionOrders() {
    try {
        sessionStorage.setItem(ORDERS_STORAGE_KEY, JSON.stringify(state.sessionOrders));
    } catch (error) {
        console.error('Could not save session orders:', error);
    }
}

function recordSessionOrder(order) {
    state.sessionOrders.unshift({ ...order, confirmation: null });
    saveSessionOrders();
    renderOrdersDrawer();
}

function updateSessionOrder(orderId, changes) {
    const order = state.sessionOrders.find(o => o.id === orderId);
    if (!order) return;
    Object.assign(order, changes);
    saveSessionOrders();
    renderOrdersDrawer();
}

function renderOrdersDrawer() {
    document.querySelectorAll('#orders-count-badge, #orders-count-badge-drawer')
        .forEach(badge => { badge.innerText = state.sessionOrders.length; });

    const list = byId('orders-list');
    if (state.sessionOrders.length === 0) {
        list.innerHTML = '<p class="orders-empty">No orders placed yet this session.</p>';
        return;
    }
    list.innerHTML = state.sessionOrders.map(order => {
        const placedAt = new Date(order.completedAt || order.createdAt).toLocaleString();
        const items = (order.items || []).map(item => `
            <div class="order-item-row">
                <span>${escapeHtml(item.cakeName)} &times; ${item.quantity}</span>
                <span>$${formatPrice(item.lineTotal)}</span>
            </div>`).join('');
        return `
            <div class="order-history-card">
                <div class="order-history-header">
                    <span>Order #${order.id}</span>
                    <span class="order-status ${order.status === 'COMPLETED' ? 'completed' : ''}">${escapeHtml(order.status)}</span>
                </div>
                <div class="order-time">${escapeHtml(placedAt)}</div>
                ${items}
                <div class="order-history-total">Total: $${formatPrice(order.totalAmount)}</div>
                ${renderConfirmation(order.confirmation)}
            </div>`;
    }).join('');
}

function renderConfirmation(confirmation) {
    if (!confirmation) {
        return '<div class="order-confirmation">Confirmation: waiting...</div>';
    }
    if (confirmation.status === 'SENT') {
        return `<div class="order-confirmation sent">Confirmation sent: ${escapeHtml(confirmation.message)}</div>`;
    }
    return '<div class="order-confirmation">Confirmation pending. It will be sent once the notification service catches up.</div>';
}

function openOrdersPanel() {
    refreshPendingConfirmations();
    byId('orders-drawer').classList.add('open');
    byId('orders-overlay').classList.add('open');
}

function closeOrdersPanel() {
    byId('orders-drawer').classList.remove('open');
    byId('orders-overlay').classList.remove('open');
}

/* ---------- Ratings ---------- */

/** Clicking the selected star again clears the selection. */
function setRating(value) {
    state.ratingScore = state.ratingScore === value ? 0 : value;
    document.querySelectorAll('.star').forEach(star => {
        const active = Number(star.dataset.value) <= state.ratingScore;
        star.classList.toggle('active', active);
        star.setAttribute('aria-checked', String(Number(star.dataset.value) === state.ratingScore));
    });
}

async function submitRating() {
    const cakeId = byId('rate-cake').value;
    const reviewText = byId('rate-review').value.trim();
    const button = byId('submit-rating');

    if (!cakeId) {
        showAlert('Please choose a cake to rate.', false);
        return;
    }
    if (state.ratingScore === 0) {
        showAlert('Please select a star rating.', false);
        return;
    }

    button.disabled = true;
    button.innerText = 'Submitting...';
    try {
        await api('/ratings', {
            method: 'POST',
            body: { cakeId: Number(cakeId), score: state.ratingScore, reviewText: reviewText || null },
        });
        showAlert('Rating submitted successfully.', true);
        byId('rate-cake').value = '';
        byId('rate-review').value = '';
        setRating(state.ratingScore);
    } catch (error) {
        showAlert(`Could not submit rating: ${error.message}`, false);
    } finally {
        button.disabled = false;
        button.innerText = 'Submit';
    }
}

async function viewAverageRating(cakeId) {
    const cake = state.cakes.find(c => c.id === cakeId);
    const name = cake ? cake.name : `Cake ${cakeId}`;
    try {
        const summary = await api(`/ratings/cake/${cakeId}/average`);
        if (summary.ratingCount === 0) {
            showAlert(`${name} has no ratings yet.`, true);
        } else {
            const label = summary.ratingCount === 1 ? 'rating' : 'ratings';
            showAlert(`${name}: ${summary.averageScore.toFixed(1)} / 5 from ${summary.ratingCount} ${label}`, true);
        }
    } catch (error) {
        showAlert(`Could not load ratings: ${error.message}`, false);
    }
}

/* ---------- Notification banner ---------- */

/** Shows a message in the top-right corner for 3 seconds. */
function showAlert(message, isSuccess) {
    const banner = byId('status-banner');
    clearTimeout(alertTimer);
    clearTimeout(alertHideTimer);

    banner.innerText = message;
    banner.className = isSuccess ? 'alert success' : 'alert';
    banner.style.display = 'block';
    // Force a reflow so the slide-in animation restarts for every message.
    void banner.offsetWidth;

    alertTimer = setTimeout(() => {
        banner.classList.add('hide');
        alertHideTimer = setTimeout(() => {
            banner.style.display = 'none';
            banner.classList.remove('hide');
        }, 400);
    }, 3000);
}
