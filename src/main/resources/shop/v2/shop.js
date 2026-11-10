// Release 2 of the demo shop: same behaviour as release 1, after a front-end redesign.
// The tests were written against release 1; every locator they use was renamed or moved here.
const USERS = {
  "alex@example.com": { password: "Healing#2026", locked: false },
  "locked@example.com": { password: "Healing#2026", locked: true }
};
const PRODUCTS = [
  { id: "torque-wrench", name: "Torque wrench", price: 49.0 },
  { id: "socket-set", name: "Socket set", price: 35.5 },
  { id: "spirit-level", name: "Spirit level", price: 12.25 }
];

function cart() { return JSON.parse(sessionStorage.getItem("cart") || "[]"); }
function saveCart(items) { sessionStorage.setItem("cart", JSON.stringify(items)); }
function money(value) { return "€" + value.toFixed(2); }
function requireLogin() { if (!sessionStorage.getItem("user")) location.href = "index.html"; }

function login(form, showError) {
  const email = form.email.value.trim();
  const user = USERS[email];
  if (!user || user.password !== form.password.value) return showError("Email or password is wrong.");
  if (user.locked) return showError("This account is locked.");
  sessionStorage.setItem("user", email);
  saveCart([]);
  location.href = "products.html";
}

function logout() { sessionStorage.clear(); location.href = "index.html"; }

function addToCart(id) {
  const items = cart();
  if (!items.includes(id)) items.push(id);
  saveCart(items);
}

function cartTotal() {
  return cart().map(id => PRODUCTS.find(p => p.id === id).price).reduce((a, b) => a + b, 0);
}

function placeOrder() {
  sessionStorage.setItem("lastOrder", JSON.stringify({ number: "HS-" + (1000 + cart().length * 7), total: cartTotal() }));
  saveCart([]);
  location.href = "confirmation.html";
}
