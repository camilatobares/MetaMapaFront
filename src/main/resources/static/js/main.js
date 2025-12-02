// Objeto para manejar el estado de la sesión.
const AppState = {
    currentUser: JSON.parse(sessionStorage.getItem('userJson')) || null
};

// Función para cerrar la sesión del usuario.
function logout() {
    sessionStorage.clear();
    window.location.href = '/logout';
}

function renderSidebar(currentPage) {
    const { currentUser } = AppState;
    let userMenu = '';

    if (currentUser) {
        userMenu = `<li class="nav-item mb-2"><a href="/profile" class="nav-link w-100 text-start py-2 rounded-3 ${currentPage === 'profile' ? 'active' : ''}"><i class="bi bi-person-circle me-2"></i> Mi Perfil</a></li>`;
    }

    let navLinks = `<li class="nav-item mb-2"><a href="/" class="nav-link w-100 text-start py-2 rounded-3 ${currentPage === 'index' ? 'active' : ''}"><i class="bi bi-grid me-2"></i> Colecciones</a></li>`;

    if (currentUser) {
        if (currentUser.role === 'admin') {
            navLinks += `<li class="nav-item mb-2"><a href="/admin" class="nav-link w-100 text-start py-2 rounded-3 ${currentPage === 'admin' ? 'active' : ''}"><i class="bi bi-person-gear me-2"></i> Administración</a></li>`;
            navLinks += `<li class="nav-item mb-2"><a href="/contributor" class="nav-link w-100 text-start py-2 rounded-3 ${currentPage === 'contributor' ? 'active' : ''}"><i class="bi bi-person-workspace me-2"></i> Mis Contribuciones </a></li>`;
        } else if (currentUser.role === 'contributor') {
            navLinks += `<li class="nav-item mb-2"><a href="/contributor" class="nav-link w-100 text-start py-2 rounded-3 ${currentPage === 'contributor' ? 'active' : ''}"><i class="bi bi-person-workspace me-2"></i> Mi Panel</a></li>`;
        }

        navLinks += `${userMenu}<li class="nav-item mt-auto"><button id="logout-btn" class="nav-link w-100 text-start py-2 rounded-3 text-danger"><i class="bi bi-box-arrow-left me-2"></i> Cerrar Sesión</button></li>`;
    } else {
        navLinks += `<li class="nav-item mb-2"><a href="/login" class="nav-link w-100 text-start py-2 rounded-3 ${currentPage === 'login' ? 'active' : ''}"><i class="bi bi-box-arrow-in-right me-2"></i> Iniciar Sesión</a></li>`;
    }

    const termsLink = `<li class="nav-item"><a href="#" id="terms-link" class="nav-link w-100 text-start py-2 rounded-3"><i class="bi bi-file-text me-2"></i> Términos y Condiciones</a></li>`;

    const imagePath = "/assets/logo.png";
    const sidebar = document.getElementById('sidebar');
    sidebar.innerHTML = `<div class="d-flex flex-column h-100"><div class="sidebar-header d-flex align-items-center mb-4 pb-3 border-bottom"><img src="${imagePath}" alt="MetaMapa Logo" class="logo-sidebar" style="border-radius: 100%;"><h2 class="display-9 fw-bold text-primary mb-0">MetaMapa</h2></div><ul class="nav nav-pills flex-column flex-grow-1">${navLinks}</ul><div class="border-top pt-3 mt-auto">${termsLink}<small class="text-muted d-block mt-2">DSI - 2025 (mi-no-grupo-24)</small></div></div>`;

    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) logoutBtn.addEventListener('click', logout);
    document.getElementById('terms-link').addEventListener('click', (e) => {
        e.preventDefault();
        openModal(renderTermsModal);
    });
}
// --- FUNCIONES PARA RENDERIZAR MODALES  ---

// ... dentro de main.js ...

function openModal(modalRenderFunc, ...args) {
    const modalContainer = document.getElementById('modal-container');
    modalContainer.innerHTML = '';
    modalRenderFunc(modalContainer, ...args);
    const modalElement = modalContainer.querySelector('.modal');
    if (modalElement) {
        const modal = new bootstrap.Modal(modalElement);
        modal.show();
        modalElement.addEventListener('hidden.bs.modal', () => { modalContainer.innerHTML = ''; }, { once: true });

        // --- CAMBIO AQUÍ: Devolver el elemento DOM, no la instancia ---
        return modalElement;
    }
    return null;
}

// Modal de Términos y Condiciones
function renderTermsModal(container) {
    container.innerHTML = `
        <div class="modal fade" id="terms-modal" tabindex="-1" aria-labelledby="termsModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-lg">
                <div class="modal-content rounded-4 shadow-lg border-0">
                    <div class="modal-header bg-primary text-white">
                        <h5 class="modal-title fw-bold" id="termsModalLabel">Términos y Condiciones de Uso</h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body p-4">
                        <p class="fw-bold">1. Aceptación de los Términos</p>
                        <p class="text-muted">Al acceder y utilizar la plataforma MetaMapa (en adelante, "la Plataforma"), usted acepta y acuerda estar sujeto a los siguientes términos y condiciones. Si no está de acuerdo con alguna parte de los términos, no podrá utilizar nuestros servicios.</p>

                        <p class="fw-bold mt-4">2. Uso de la Plataforma</p>
                        <p class="text-muted">Usted se compromete a utilizar la Plataforma de manera responsable y con fines lícitos. Queda prohibido subir contenido que sea falso, difamatorio, ilegal, que incite al odio o que viole los derechos de terceros.</p>

                        <p class="fw-bold mt-4">3. Contenido del Usuario</p>
                        <p class="text-muted">Al subir un "Hecho" o cualquier otro contenido, usted otorga a la Plataforma una licencia no exclusiva, mundial y libre de regalías para usar, reproducir y mostrar dicho contenido. Usted declara ser el propietario de los derechos del contenido que aporta o tener los permisos necesarios para ello.</p>

                        <p class="fw-bold mt-4">4. Proceso de Verificación</p>
                        <p class="text-muted">Todo el contenido sugerido por los contribuyentes está sujeto a un proceso de revisión y verificación por parte de los administradores de la Plataforma. MetaMapa se reserva el derecho de aceptar, rechazar o eliminar cualquier contenido a su entera discreción sin previo aviso.</p>
                        
                        <p class="fw-bold mt-4">5. Limitación de Responsabilidad</p>
                        <p class="text-muted">La información presentada en la Plataforma se proporciona "tal cual" y se basa en las contribuciones de la comunidad. MetaMapa no garantiza la exactitud, integridad o actualidad de la información y no se hace responsable de las decisiones tomadas con base en el contenido del sitio.</p>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-primary rounded-pill px-4" data-bs-dismiss="modal">Entendido</button>
                    </div>
                </div>
            </div>
        </div>`;
}

function renderFactDetailModal(container, fact) {
    const formattedDate = new Date(fact.fechaAcontecimiento).toLocaleDateString('es-ES', { year: 'numeric', month: 'long', day: 'numeric' });
    const fuenteTexto = fact.nombreOrigen || 'No especificada';

    // --- LÓGICA DE MULTIMEDIA ---
    // --- LÓGICA DE MULTIMEDIA CORREGIDA ---
    let imageHtml = '';
    if (fact.contenidoMultimedia) {
        let imageUrl = fact.contenidoMultimedia;

        // Aseguramos que siempre apunte al backend de Fuente Dinámica (puerto 8082)
        // Si la URL no incluye el dominio correcto, se lo forzamos.
        const baseUrlBackend = 'http://localhost:8082';

        if (!imageUrl.includes(baseUrlBackend)) {
            // Quitamos cualquier barra inicial o 'http://...' incorrecto que pueda tener
            imageUrl = imageUrl.replace(/^(https?:\/\/[^\/]+)?\//, '');
            // Construimos la URL final correcta
            imageUrl = `${baseUrlBackend}/uploads/${imageUrl}`;
        }

        imageHtml = `
        <div class="mb-4 text-center bg-light p-2 rounded border">
            <img src="${imageUrl}" 
                 class="img-fluid rounded shadow-sm" 
                 style="max-height: 350px; width: auto;" 
                 alt="Evidencia del hecho"
                 onerror="this.style.display='none'; this.nextElementSibling.style.display='block';">
            <div class="text-muted small mt-2" style="display:none;">
                <i class="bi bi-exclamation-triangle me-1"></i> No se pudo cargar la imagen.
            </div>
        </div>`;
    }
// ---------------------------------------

    container.innerHTML = `
        <div class="modal fade" id="fact-detail-modal" tabindex="-1">
            <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable"> 
                <div class="modal-content rounded-4 shadow-lg border-0">
                    <div class="modal-header bg-light border-bottom-0">
                        <div>
                            <h5 class="modal-title text-primary fw-bold">${fact.titulo}</h5>
                            <span class="badge bg-secondary">${fact.categoria}</span>
                        </div>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body p-4">
                        

                        <h6 class="fw-bold text-dark border-bottom pb-2 mb-3">Descripción</h6>
                        <p class="text-muted" style="white-space: pre-wrap; overflow-wrap: break-word;">${fact.descripcion}</p>

                        <div class="row mt-4 pt-3 border-top">
                            <div class="col-md-6">
                                <h6 class="fw-bold mb-3">Información Adicional</h6>
                                <ul class="list-unstyled text-muted small">
                                    <li class="mb-2"><strong><i class="bi bi-calendar-event me-2 text-primary"></i>Fecha:</strong> ${formattedDate}</li>
                                    <li class="mb-2"><strong><i class="bi bi-hdd-network me-2 text-primary"></i>Fuente:</strong> ${fuenteTexto}</li>
                                    <li><strong><i class="bi bi-person-check me-2 text-primary"></i>Consenso:</strong> ${fact.consensuado ? '<span class="text-success">Verificado</span>' : '<span class="text-warning">En revisión</span>'}</li>
                                </ul>
                            </div>
                            <div class="col-md-6">
                                <h6 class="fw-bold mb-2">Ubicación</h6>
                                <div id="mini-map" style="height: 180px; width: 100%; border-radius: 8px; background-color: #eee; border: 1px solid #ddd;"></div>
                            </div>
                        </div>
                        <div class="row mt-4 pt-3 border-top">
                            ${imageHtml}
                        </div>                     
                    </div>
                    <div class="modal-footer bg-light">
                        <button type="button" class="btn btn-outline-secondary rounded-pill px-4" data-bs-dismiss="modal">Cerrar</button>
                    </div>
                </div>
            </div>
        </div>`;
}

// =========================================================================================
// FUNCIÓN renderRequestDetailModal (Crítica para el Admin/Contributor)
// =========================================================================================

function renderRequestDetailModal(container, requestData) {

    const requestType = requestData.tipo || (requestData.idHechoOriginal ? 'Edición' : (requestData.motivo ? 'Eliminación' : 'Nuevo Hecho'));
    const requestEstado = requestData.estado || 'N/A';
    const isEdicion = requestType === 'Edición';
    const isEliminacion = requestType === 'Eliminación';

    // Campos primarios
    const titulo = requestData.titulo || requestData.tituloPropuesto || requestData.tituloDelHechoAEliminar || requestData.nombreHecho || `Hecho ID ${requestData.idHechoOriginal || requestData.id}`;
    const descripcionPropuesta = requestData.descripcionPropuesta || requestData.descripcion || '';
    const motivoEliminacion = requestData.motivo || '';

    // Feedback Admin
    let adminFeedbackHtml = '';
    const mensajeAdmin = requestData.sugerenciaAdmin || requestData.detalle || '';

    if (requestEstado === 'ACEPTADO_CON_SUGERENCIAS') {
        adminFeedbackHtml = `<div class="alert alert-warning border-warning shadow-sm mb-3"><div class="d-flex"><i class="bi bi-lightbulb-fill fs-4 me-3"></i><div><h6 class="alert-heading fw-bold mb-1">Sugerencia del Administrador:</h6><p class="mb-0 text-dark">${mensajeAdmin || 'Sin detalles.'}</p></div></div></div>`;
    } else if (requestEstado === 'RECHAZADO' && mensajeAdmin) {
        adminFeedbackHtml = `<div class="alert alert-danger border-danger shadow-sm mb-3"><h6 class="alert-heading fw-bold"><i class="bi bi-x-circle me-2"></i>Motivo del Rechazo:</h6><p class="mb-0">${mensajeAdmin}</p></div>`;
    }

    // Helper para formato de fecha
    const formatF = (dateStr) => {
        if (!dateStr) return '-';
        return new Date(dateStr).toLocaleString('es-AR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    };

    let propuestaContent = '';
    let mapsToInit = []; // Array para guardar los mapas a inicializar

    // ============================================================
    // 1. ELIMINACIÓN
    // ============================================================
    if (isEliminacion) {
        propuestaContent += `<p class="fw-bold text-danger">Motivo de Eliminación:</p><p class="text-muted" style="white-space: pre-wrap;">${motivoEliminacion || 'No especificado.'}</p><p><strong>Título del Hecho a Eliminar:</strong> ${requestData.nombreHecho || titulo}</p>`;
    }
        // ============================================================
        // 2. EDICIÓN (Aquí agregamos FECHA y TITULOS DE MAPA)
    // ============================================================
    else if (isEdicion) {
        const orig = requestData.original || {};
        const prop = requestData.propuesta ? requestData.propuesta : requestData;

        const isDiff = (a, b) => a !== b ? 'text-success fw-bold' : '';

        // --- FIX 2: FECHAS ---
        const fechaO = formatF(orig.fechaAcontecimiento);
        const fechaP = formatF(prop.fechaAcontecimientoPropuesta);

        // Imágenes
        const imgOrig = orig.contenidoMultimedia ?
            `<div class="mt-2"><small class="fw-bold text-muted">Multimedia Actual:</small><br><img src="${orig.contenidoMultimedia}" class="img-fluid rounded shadow-sm" style="max-height: 150px;" onerror="this.style.display='none';"></div>`
            : `<div class="mt-2 text-muted small">Sin multimedia original.</div>`;

        const imgProp = prop.contenidoMultimediaPropuesto ?
            `<div class="mt-2"><small class="fw-bold text-success">Nueva Imagen:</small><br><img src="${prop.contenidoMultimediaPropuesto}" class="img-fluid rounded shadow-sm" style="max-height: 150px;" onerror="this.style.display='none';"></div>`
            : `<div class="mt-2 text-muted small fst-italic border-top pt-1">Sin cambios en multimedia.</div>`;

        propuestaContent = `
        <div class="alert alert-warning border-0 shadow-sm mb-3">Propuesta de Edición</div>
        <div class="row g-0 border rounded mb-3">
            <div class="col-6 border-end bg-light p-3">
                <h6 class="text-center text-muted small fw-bold text-uppercase mb-3">Original</h6>
                
                <div class="mb-2"><small class="fw-bold d-block text-secondary">Título:</small><div>${orig.titulo || '-'}</div></div>
                <div class="mb-2"><small class="fw-bold d-block text-secondary">Descripción:</small><div class="small">${orig.descripcion || '-'}</div></div>
                <div class="mb-2"><small class="fw-bold d-block text-secondary">Categoría:</small><div class="small">${orig.categoria || '-'}</div></div>
                
                <div class="mb-2"><small class="fw-bold d-block text-secondary">Fecha:</small><div class="small">${fechaO}</div></div>
                
                ${imgOrig}
            </div>
            
            <div class="col-6 bg-white p-3">
                <h6 class="text-center text-primary small fw-bold text-uppercase mb-3">Propuesta</h6>
                
                <div class="mb-2"><small class="fw-bold d-block text-secondary">Título:</small><div class="${isDiff(orig.titulo, prop.tituloPropuesto)}">${prop.tituloPropuesto || orig.titulo}</div></div>
                <div class="mb-2"><small class="fw-bold d-block text-secondary">Descripción:</small><div class="${isDiff(orig.descripcion, prop.descripcionPropuesta)} small">${prop.descripcionPropuesta || orig.descripcion}</div></div>
                <div class="mb-2"><small class="fw-bold d-block text-secondary">Categoría:</small><div class="${isDiff(orig.categoria, prop.categoriaPropuestaNombre || prop.categoriaPropuesta)} small">${prop.categoriaPropuestaNombre || prop.categoriaPropuesta || orig.categoria}</div></div>
                
                <div class="mb-2"><small class="fw-bold d-block text-secondary">Fecha:</small><div class="${isDiff(fechaO, fechaP)} small">${fechaP}</div></div>
                
                ${imgProp}
            </div>
        </div>`;

        // --- FIX 1: MAPAS CON TÍTULO ---
        const latO = parseFloat(orig.latitud); const lngO = parseFloat(orig.longitud);
        const latP = parseFloat(prop.latitudPropuesta); const lngP = parseFloat(prop.longitudPropuesta);

        if ((!isNaN(latO) && !isNaN(lngO)) && (!isNaN(latP) && !isNaN(lngP)) && (latO !== latP || lngO !== lngP)) {
            propuestaContent += `
             <div class="row g-3">
                <div class="col-6">
                    <div class="card border-danger h-100">
                        <div class="card-header bg-danger text-white small fw-bold text-center">Ubicación Original</div>
                        <div class="card-body p-0">
                            <div id="map-edicion-original" style="height: 200px; width: 100%;"></div>
                        </div>
                    </div>
                </div>
                <div class="col-6">
                    <div class="card border-success h-100">
                        <div class="card-header bg-success text-white small fw-bold text-center">Ubicación Propuesta</div>
                        <div class="card-body p-0">
                            <div id="map-edicion-propuesta" style="height: 200px; width: 100%;"></div>
                        </div>
                    </div>
                </div>
             </div>`;

            mapsToInit.push({ id: 'map-edicion-original', lat: latO, lng: lngO, popup: 'Ubicación Original' });
            mapsToInit.push({ id: 'map-edicion-propuesta', lat: latP, lng: lngP, popup: 'Nueva Ubicación' });
        } else {
            // Si no cambió la ubicación, mostramos uno solo o un mensaje
            propuestaContent += `
             <div class="alert alert-secondary d-flex align-items-center py-2 mt-3">
                <i class="bi bi-geo-alt me-2"></i> Sin cambios en la ubicación.
             </div>`;
        }

    }
        // ============================================================
        // 3. NUEVO HECHO
    // ============================================================
    else {
        const lat = requestData.latitud || requestData.latitudPropuesta;
        const lon = requestData.longitud || requestData.longitudPropuesta;
        const fechaRaw = requestData.fechaAcontecimiento || requestData.fechaAcontecimientoPropuesta;
        const categoriaNombre = requestData.categoria || (requestData.categoriaPropuesta && requestData.categoriaPropuesta.nombre) || 'No especificada';

        propuestaContent += `<p class="fw-bold text-primary">Detalles del Hecho</p>
        <p><strong>Descripción:</strong> ${descripcionPropuesta}</p>
        <ul class="list-unstyled small">
            <li><strong>Título:</strong> ${titulo}</li>
            <li><strong>Categoría:</strong> <span class="badge bg-secondary">${categoriaNombre}</span></li>
            <li><strong>Fecha:</strong> ${formatF(fechaRaw)}</li>
        </ul>`;

        if (lat && lon) {
            propuestaContent += `<div class="card"><div class="card-header small fw-bold">Ubicación</div><div class="card-body p-0"><div id="request-map-container" style="height: 200px; width: 100%;"></div></div></div>`;
            mapsToInit.push({ id: 'request-map-container', lat: parseFloat(lat), lng: parseFloat(lon), popup: 'Ubicación' });
        }
    }

    const statusClass = requestEstado === 'PENDIENTE' ? 'bg-warning' : (requestEstado.includes('APROB') || requestEstado.includes('ACEPT') ? 'bg-success' : 'bg-danger');

    container.innerHTML = `
        <div class="modal fade" id="request-detail-modal" tabindex="-1" aria-hidden="true">
            <div class="modal-dialog modal-xl modal-dialog-scrollable modal-dialog-centered">
                <div class="modal-content rounded-4 shadow-lg border-0">
                    <div class="modal-header bg-primary text-white">
                        <h5 class="modal-title fw-bold">Detalle de Solicitud</h5>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body p-4">
                        <div class="d-flex justify-content-between align-items-center mb-3">
                            <h6 class="fw-bold mb-0">Tipo: <span class="badge bg-primary">${requestType}</span></h6>
                            <h6 class="fw-bold mb-0">Estado: <span class="badge ${statusClass}">${requestEstado}</span></h6>
                        </div>
                        ${adminFeedbackHtml}
                        <div class="card card-body bg-light mb-3 border-0 shadow-sm">${propuestaContent}</div>
                        <small class="text-muted d-block mt-3 text-end">ID Solicitud: ${requestData.id}</small>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary rounded-pill px-4" data-bs-dismiss="modal">Cerrar</button>
                    </div>
                </div>
            </div>
        </div>`;

    // --- INICIALIZACIÓN DE MAPAS ---
    if (mapsToInit.length > 0) {
        // Esperamos un poco a que el modal se renderice en el DOM
        setTimeout(() => {
            mapsToInit.forEach(m => {
                const el = document.getElementById(m.id);
                if(el) {
                    const map = L.map(m.id).setView([m.lat, m.lng], 13);
                    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);
                    L.marker([m.lat, m.lng]).addTo(map).bindPopup(m.popup);
                    // Forzar redraw
                    setTimeout(() => { map.invalidateSize(); }, 200);
                }
            });
        }, 500); // Delay para asegurar que el modal esté visible
    }
}

// --- INICIALIZACIÓN ---
document.addEventListener('DOMContentLoaded', () => {
    const toggler = document.getElementById('sidebar-toggler-btn');
    const sidebar = document.getElementById('sidebar');
    if (toggler) toggler.addEventListener('click', () => sidebar.classList.toggle('show'));
});