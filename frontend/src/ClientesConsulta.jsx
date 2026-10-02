import React, { useEffect, useState } from 'react';
import { ArrowLeft, Search, UserPlus, Users } from 'lucide-react';
import './clientes-consulta.css';

/** Ejecuta GET privado; parámetros URL y signal; devuelve JSON o lanza Error HTTP. */
async function consultar(url, signal) {
  const response = await fetch(url, { signal, headers: { Authorization: `Bearer ${localStorage.getItem('taller_token')}` } });
  if (!response.ok) throw new Error(response.status === 403 ? 'No tienes permiso para consultar clientes.' :
    response.status === 404 ? 'Cliente no encontrado.' : 'No se pudieron cargar los clientes. Intenta nuevamente.');
  return response.json();
}

/** Avatar privado; recibe id/nombre y muestra iniciales si falta foto. Devuelve JSX; errores visuales recuperables. */
function FotoCliente({ id, nombre }) {
  const [url, setUrl] = useState('');
  useEffect(() => {
    const controller = new AbortController(); let objectUrl = '';
    /** Carga bytes con Bearer; devuelve Promise<void>; presenta iniciales ante un fallo. */
    async function cargar() {
      try {
        const response = await fetch(`/api/clientes/${id}/fotografia`, {
          signal: controller.signal, headers: { Authorization: `Bearer ${localStorage.getItem('taller_token')}` },
        });
        if (!response.ok) return;
        const blob = await response.blob();
        if (controller.signal.aborted) return;
        objectUrl = URL.createObjectURL(blob); setUrl(objectUrl);
      } catch { /* Las iniciales conservan la identidad cuando no hay fotografía. */ }
    }
    setUrl(''); cargar();
    return () => { controller.abort(); if (objectUrl) URL.revokeObjectURL(objectUrl); };
  }, [id]);
  return <span className="cliente-avatar">{url ? <img src={url} alt={`Fotografía de ${nombre}`} /> :
    <span aria-label={`Sin fotografía: ${nombre}`}>{nombre.trim().split(/\s+/).slice(0, 2).map(word => word[0]).join('').toUpperCase()}</span>}</span>;
}

/** Listado y detalle de solo lectura; recibe callbacks volver/registrar; devuelve JSX. Los errores GET son visibles. */
export default function ClientesConsulta({ volver, registrar }) {
  const [texto, setTexto] = useState(''); const [busqueda, setBusqueda] = useState('');
  const [pagina, setPagina] = useState(0); const [id, setId] = useState(null);
  const [resultado, setResultado] = useState(null); const [detalle, setDetalle] = useState(null);
  const [error, setError] = useState(''); const [cargando, setCargando] = useState(true);
  const [intento, setIntento] = useState(0);
  useEffect(() => {
    const controller = new AbortController(); setCargando(true); setError(''); setDetalle(null);
    /** Solicita la página o detalle seleccionado; devuelve Promise<void>; captura errores para la alerta. */
    async function cargar() {
      try {
        const params = new URLSearchParams({ pagina: String(pagina), tamanio: '10', busqueda });
        const data = await consultar(id ? `/api/clientes/${id}` : `/api/clientes?${params}`, controller.signal);
        if (!controller.signal.aborted) { if (id) setDetalle(data); else setResultado(data); }
      } catch (exception) { if (!controller.signal.aborted) setError(exception.message); }
      finally { if (!controller.signal.aborted) setCargando(false); }
    }
    cargar(); return () => controller.abort();
  }, [pagina, busqueda, id, intento]);
  /** Aplica búsqueda y vuelve a primera página; recibe submit; devuelve void. */
  function buscar(event) { event.preventDefault(); setPagina(0); setBusqueda(texto.trim()); }
  return <main className="consulta-page"><div className="consulta-shell">
    <button className="text-button" onClick={() => id ? setId(null) : volver()}><ArrowLeft size={18} />{id ? 'Volver al listado' : 'Volver al panel'}</button>
    <section className="consulta-card" aria-busy={cargando}>
      <header className="consulta-heading"><div><p className="eyebrow">Directorio del taller</p>
        <h1>{id ? 'Detalle del cliente' : 'Clientes registrados'}</h1>
        <p className="consulta-muted">{id ? 'Expediente de solo lectura' : 'Encuentra los datos de contacto de tus clientes.'}</p></div>
        <button className="primary-button inline-button" onClick={registrar}><UserPlus size={18} />Registrar cliente</button>
      </header>
      {!id && <><form className="consulta-search" onSubmit={buscar}>
        <label htmlFor="clientes-busqueda">Buscar por nombre, teléfono o e-mail</label>
        <div><input id="clientes-busqueda" value={texto} maxLength={120} onChange={event => setTexto(event.target.value)} placeholder="Escribe el dato del cliente" />
        <button className="primary-button inline-button" type="submit"><Search size={18} />Buscar</button></div>
      </form><p className="consulta-muted consulta-note">Orden: ID más reciente primero. Fecha de registro no disponible en los datos actuales.</p></>}
      {error && <div className="notice notice-error" role="alert">{error}<button className="text-button" onClick={() => setIntento(intento + 1)}>Reintentar</button></div>}
      {cargando && <p role="status" className="consulta-loading"><span className="spinner" aria-hidden="true" />Cargando clientes…</p>}
      {!cargando && !error && !id && resultado && <>
        {resultado.clientes.length === 0 ? <div className="consulta-empty" role="status"><Users size={32} /><h2>{busqueda ? 'No hay clientes que coincidan con tu búsqueda' : 'Aún no hay clientes registrados'}</h2><p>Registra un cliente o prueba otra búsqueda.</p></div> :
          <table className="clientes-table"><caption className="sr-only">Clientes registrados, página {pagina + 1}</caption>
            <thead><tr>{['Cliente', 'Teléfono personal', 'E-mail', 'Municipio', 'Fecha de registro', 'Detalle'].map(label => <th key={label} scope="col">{label}</th>)}</tr></thead>
            <tbody>{resultado.clientes.map(cliente => <tr key={cliente.id}>
              <td data-label="Cliente"><div className="cliente-identity"><FotoCliente id={cliente.id} nombre={cliente.nombreCompleto} /><strong>{cliente.nombreCompleto}</strong></div></td>
              <td data-label="Teléfono personal">{cliente.telefonoPersonal}</td><td data-label="E-mail">{cliente.email}</td><td data-label="Municipio">{cliente.municipio}</td>
              <td data-label="Fecha de registro"><span className="consulta-badge">No disponible</span></td>
              <td><button className="text-button consulta-open" onClick={() => setId(cliente.id)} aria-label={`Ver detalle de ${cliente.nombreCompleto}`}>Ver detalle →</button></td>
            </tr>)}</tbody></table>}
        <nav className="consulta-pagination" aria-label="Paginación de clientes"><span>{resultado.total} clientes · Página {pagina + 1} de {Math.max(1, resultado.paginas)}</span>
          <div><button className="text-button" disabled={pagina === 0} onClick={() => setPagina(pagina - 1)}>Anterior</button>
          <button className="text-button" disabled={pagina + 1 >= resultado.paginas} onClick={() => setPagina(pagina + 1)}>Siguiente</button></div></nav>
      </>}
      {!cargando && !error && detalle && <><div className="detalle-identity"><FotoCliente id={detalle.id} nombre={detalle.nombreCompleto} /><h2>{detalle.nombreCompleto}</h2><span className="consulta-badge">Solo lectura</span></div>
        <dl className="detalle-grid">{[
          ['Nombre completo', detalle.nombreCompleto], ['Contacto alternativo', detalle.contactoAlternativo],
          ['Edad registrada', detalle.edad], ['Fecha de nacimiento', detalle.fechaNacimiento],
          ['Teléfono personal', detalle.telefonoPersonal], ['Teléfono de trabajo', detalle.telefonoTrabajo],
          ['E-mail', detalle.email], ...(detalle.emailTrabajo ? [['E-mail de trabajo', detalle.emailTrabajo]] : []),
          ['Calle', detalle.calle], ['Colonia', detalle.colonia], ['Municipio', detalle.municipio],
          ['Estado', detalle.estado], ['Código postal', detalle.codigoPostal],
        ].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl></>}
    </section>
  </div></main>;
}
