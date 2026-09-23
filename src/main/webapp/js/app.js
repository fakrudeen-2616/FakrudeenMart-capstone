(() => {
  'use strict';
  const root = document.documentElement;
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (!reduceMotion) root.classList.add('motion-ready');

  document.querySelectorAll('.site-header, .admin-header').forEach(header => {
    const nav = header.querySelector('nav');
    if (!nav || nav.dataset.enhanced === 'true') return;
    nav.dataset.enhanced = 'true';
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'menu-toggle';
    button.setAttribute('aria-expanded', 'false');
    button.setAttribute('aria-controls', 'primary-navigation');
    button.innerHTML = '<span class="sr-only">Toggle navigation</span><span aria-hidden="true">☰</span>';
    nav.id = 'primary-navigation';
    header.insertBefore(button, nav);
    const links = Array.from(nav.querySelectorAll('a')).map(link => link.getAttribute('href') || '');
    const brandHref = header.querySelector('.brand')?.getAttribute('href');
    if (brandHref && !links.includes(brandHref)) {
      const home = document.createElement('a'); home.href = brandHref; home.textContent = 'Home'; nav.insertBefore(home, nav.firstChild);
    }
    if (links.some(href => href.endsWith('/cart')) && !links.some(href => href.endsWith('/orders'))) {
      const orders = document.createElement('a'); orders.href = `${document.querySelector('a[href$="/cart"]')?.href.replace(/\/cart$/, '') || ''}/orders`; orders.textContent = 'Orders'; nav.appendChild(orders);
    }
    button.addEventListener('click', () => {
      const open = nav.classList.toggle('is-open');
      button.setAttribute('aria-expanded', String(open));
    });
    nav.addEventListener('click', event => {
      if (event.target.closest('a')) nav.classList.remove('is-open');
    });
  });

  document.querySelectorAll('.notice').forEach(notice => {
    if (notice.querySelector('.notice-dismiss')) return;
    const close = document.createElement('button');
    close.type = 'button'; close.className = 'notice-dismiss';
    close.setAttribute('aria-label', 'Dismiss message'); close.textContent = '×';
    close.addEventListener('click', () => notice.remove());
    notice.appendChild(close);
  });

  document.querySelectorAll('.status-pill').forEach(badge => {
    const status = badge.textContent.trim().toLowerCase().replace(/\s+/g, '-');
    if (status) badge.classList.add(`status-${status}`);
  });

  document.querySelectorAll('.catalog-filters').forEach(form => {
    if (form.querySelector('.clear-filters')) return;
    const clear = document.createElement('a'); clear.className = 'clear-filters'; clear.href = form.getAttribute('action') || '/products'; clear.textContent = 'Clear filters';
    form.appendChild(clear);
  });

  document.querySelectorAll('input[type="number"]').forEach(input => {
    input.addEventListener('input', () => {
      if (input.min && Number(input.value) < Number(input.min)) input.setCustomValidity(`Value must be at least ${input.min}.`);
      else if (input.max && Number(input.value) > Number(input.max)) input.setCustomValidity(`Value must be at most ${input.max}.`);
      else input.setCustomValidity('');
    });
  });

  const main = document.querySelector('main');
  if (main && !document.querySelector('.site-footer')) {
    const footer = document.createElement('footer');
    footer.className = 'site-footer';
    footer.innerHTML = '<span>FakrudeenMart</span><span>Thoughtful shopping from independent sellers.</span>';
    main.appendChild(footer);
  }
})();
