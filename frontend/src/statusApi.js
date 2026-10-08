/** Ejecuta una solicitud autenticada al catálogo de estatus. @param {string} path Ruta relativa. @param {RequestInit} options Opciones HTTP. @returns {Promise<unknown>} Respuesta JSON. @throws {Error} Cuando la operación es rechazada. */
async function request(path, options = {}) {
  const response = await fetch(`/api/status${path}`, {
    ...options,
    headers: {
      Authorization: `Bearer ${localStorage.getItem('taller_token') || ''}`,
      ...(options.body ? { 'Content-Type': 'application/json' } : {}),
      ...options.headers,
    },
  });
  const text = await response.text();
  let data = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }
  if (!response.ok) {
    const mensajes = { 400: 'Los datos del estatus no son válidos.', 403: 'No tienes permiso para administrar estatus.', 404: 'El estatus solicitado no existe.', 409: 'El estatus está duplicado o está siendo utilizado.' };
    throw new Error(data?.message || mensajes[response.status] || 'No fue posible completar la operación de estatus.');
  }
  return data;
}

/** Servicio API para listar y gestionar el catálogo de estatus. */
export const statusApi = {
  /** @returns {Promise<object>} Página solicitada al servidor. */
  listar(pagina = 0, tamanio = 10, orden = 'id', direccion = 'asc') {
    return request(`?${new URLSearchParams({ pagina: String(pagina), tamanio: String(Math.min(tamanio, 10)), orden, direccion })}`);
  },
  /** @param {number|string} id Identificador. @returns {Promise<object>} Estatus encontrado. */
  consultar: (id) => request(`/${id}`),
  /** @param {object} datos Datos del estatus. @returns {Promise<object>} Estatus creado. */
  crear: (datos) => request('', { method: 'POST', body: JSON.stringify(datos) }),
  /** @param {number|string} id Identificador. @param {object} datos Cambios. @returns {Promise<object>} Estatus actualizado. */
  actualizar: (id, datos) => request(`/${id}`, { method: 'PUT', body: JSON.stringify(datos) }),
  /** @param {number|string} id Identificador. @returns {Promise<null>} Respuesta vacía. */
  eliminar: (id) => request(`/${id}`, { method: 'DELETE' }),
};
