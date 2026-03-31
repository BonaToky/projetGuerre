function loadNavbar(){fetch("/navbar.html").then((e=>e.text())).then((e=>{document.body.insertAdjacentHTML("afterbegin",e);const t=window.location.pathname,n="/articles"===t||"/articles.html"===t,a=document.getElementById("navbarFilters");a&&n&&(a.style.display="block");const d=document.getElementById("navbarDate");d&&(d.textContent=(new Date).toLocaleDateString("fr-FR",{weekday:"long",day:"numeric",month:"long",year:"numeric"}));const o=document.getElementById("navbarToggle");o&&a&&o.addEventListener("click",(()=>{const e="none"!==a.style.display;a.style.display=e?"none":"block",o.classList.toggle("active",!e)}));const l=document.getElementById("navbarSettingsBtn"),i=document.getElementById("adminSidebar"),c=document.getElementById("adminSidebarClose"),s=document.getElementById("adminSidebarOverlay");function r(){i&&s&&(i.classList.remove("open"),i.style.display="none",s.style.display="none",document.body.style.overflow="",l&&(l.classList.remove("active"),l.setAttribute("aria-expanded","false")))}l&&l.addEventListener("click",(function(){i&&s&&(i.style.display="block",s.style.display="block",i.classList.add("open"),document.body.style.overflow="hidden",l&&(l.classList.add("active"),l.setAttribute("aria-expanded","true")))})),c&&c.addEventListener("click",r),s&&s.addEventListener("click",r),document.addEventListener("keydown",(e=>{"Escape"===e.key&&r()}));const m=localStorage.getItem("adminBasicAuth"),y={headers:{}};m&&(y.headers.Authorization=m),fetch("/api/admin/me",y).then((e=>{e.ok&&l&&(l.style.display="inline-flex");const t=document.getElementById("adminLogoutBtn");t&&(t.style.display=e.ok?"":"none",e.ok&&t.addEventListener("click",(async e=>{e.preventDefault();try{localStorage.removeItem("adminBasicAuth")}catch(e){}try{await fetch("/api/admin/logout",{method:"POST"})}catch(e){}r(),window.location.href="/index.html"})))})).catch((()=>{})),document.body.style.paddingTop=n?"180px":"80px",document.dispatchEvent(new CustomEvent("navbar:loaded",{detail:{isArticlesList:n}}))})).catch((e=>{console.error("Erreur lors du chargement de la navbar:",e)}))}loadNavbar();

// // load-navbar.min.js
// // Ce script charge la navbar automatiquement sur toutes les pages

// function loadNavbar() {
//   fetch('/navbar.html')
//     .then(response => response.text())
//     .then(html => {
//       // Insérer la navbar au début du body
//       document.body.insertAdjacentHTML('afterbegin', html);

//       const path = window.location.pathname;
//       const isArticlesList = path === '/articles' || path === '/articles.html';
//       const filters = document.getElementById('navbarFilters');
//       if (filters && isArticlesList) {
//         filters.style.display = 'block';
//       }

//       const dateEl = document.getElementById('navbarDate');
//       if (dateEl) {
//         dateEl.textContent = new Date().toLocaleDateString('fr-FR', {
//           weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
//         });
//       }

//       const toggle = document.getElementById('navbarToggle');
//       if (toggle && filters) {
//         toggle.addEventListener('click', () => {
//           const open = filters.style.display !== 'none';
//           filters.style.display = open ? 'none' : 'block';
//           toggle.classList.toggle('active', !open);
//         });
//       }

//       const settingsBtn = document.getElementById('navbarSettingsBtn');
//       const sidebar = document.getElementById('adminSidebar');
//       const closeBtn = document.getElementById('adminSidebarClose');
//       const overlay = document.getElementById('adminSidebarOverlay');

//       function openSidebar() {
//         if (!sidebar || !overlay) return;
//         sidebar.style.display = 'block';
//         overlay.style.display = 'block';
//         sidebar.classList.add('open');
//         document.body.style.overflow = 'hidden';
//         if (settingsBtn) {
//           settingsBtn.classList.add('active');
//           settingsBtn.setAttribute('aria-expanded', 'true');
//         }
//       }

//       function closeSidebar() {
//         if (!sidebar || !overlay) return;
//         sidebar.classList.remove('open');
//         sidebar.style.display = 'none';
//         overlay.style.display = 'none';
//         document.body.style.overflow = '';
//         if (settingsBtn) {
//           settingsBtn.classList.remove('active');
//           settingsBtn.setAttribute('aria-expanded', 'false');
//         }
//       }

//       if (settingsBtn) {
//         settingsBtn.addEventListener('click', openSidebar);
//       }
//       if (closeBtn) {
//         closeBtn.addEventListener('click', closeSidebar);
//       }
//       if (overlay) {
//         overlay.addEventListener('click', closeSidebar);
//       }
//       document.addEventListener('keydown', (event) => {
//         if (event.key === 'Escape') {
//           closeSidebar();
//         }
//       });

//       // Admin links visibility is decided by backend auth, not only UI state.
//       const adminAuth = localStorage.getItem('adminBasicAuth');
//       const adminReq = { headers: {} };
//       if (adminAuth) {
//         adminReq.headers['Authorization'] = adminAuth;
//       }
//       fetch('/api/admin/me', adminReq)
//         .then(r => {
//           if (r.ok && settingsBtn) {
//             settingsBtn.style.display = 'inline-flex';
//           }
//           // show logout button only when admin is authenticated
//           const logoutBtn = document.getElementById('adminLogoutBtn');
//           if (logoutBtn) {
//             logoutBtn.style.display = r.ok ? '' : 'none';
//             if (r.ok) {
//               logoutBtn.addEventListener('click', async (e) => {
//                 e.preventDefault();
//                 try { localStorage.removeItem('adminBasicAuth'); } catch (err) {}
//                 try { await fetch('/api/admin/logout', { method: 'POST' }); } catch (_) {}
//                 closeSidebar();
//                 window.location.href = '/index.html';
//               });
//             }
//           }
//         })
//         .catch(() => {});
      
//       // Ajouter un padding-top au body pour que le contenu ne soit pas caché sous la navbar fixe
//       document.body.style.paddingTop = isArticlesList ? '180px' : '80px';

//       // Notify pages that navbar (and optional filters) are ready.
//       document.dispatchEvent(new CustomEvent('navbar:loaded', {
//         detail: { isArticlesList }
//       }));
//     })
//     .catch(error => {
//       console.error('Erreur lors du chargement de la navbar:', error);
//     });
// }

// // Charger la navbar quand la page est prête
// loadNavbar();