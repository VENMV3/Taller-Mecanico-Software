import React, { useEffect, useState } from 'react';
import { ArrowLeft, Eye, Pencil, Search, UserPlus, Users } from 'lucide-react';
import './clientes-consulta.css';

/** Ejecuta GET privado; parámetros URL y signal; devuelve JSON o lanza Error HTTP. */
async function consultar(url, signal) {
  const response = await fetch(url, { signal, headers: { Authorization: `Bearer ${localStorage.getItem('taller_token')}` } });
  if (!response.ok) throw new Error(response.status === 403 ? 'No tienes permiso para consultar clientes.' :
    response.status === 404 ? 'Cliente no encontrado.' : 'No se pudieron cargar los clientes. Intenta nuevamente.');
  return response.json();
}

/** Avatar privado; recibe id/nombre y muestra iniciales si falta foto. Devuelve JSX; errores visuales recuperables. */
function FotoCliente({ id, nombre, tallerId }) {
  const [url, setUrl] = useState('');
  useEffect(() => {
    const controller = new AbortController(); let objectUrl = '';
    /** Carga bytes con Bearer; devuelve Promise<void>; presenta iniciales ante un fallo. */
    async function cargar() {
      try {
        const response = await fetch(`/api/clientes/${id}/fotografia${tallerId ? `?tallerId=${tallerId}` : ''}`, {
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
  }, [id, tallerId]);
  return <span className="cliente-avatar">{url ? <img src={url} alt={`Fotografía de ${nombre}`} /> :
    <span aria-label={`Sin fotografía: ${nombre}`}>{nombre.trim().split(/\s+/).slice(0, 2).map(word => word[0]).join('').toUpperCase()}</span>}</span>;
}

/** Listado y detalle de solo lectura; recibe callbacks volver/registrar; devuelve JSX. Los errores GET son visibles. */
export default function ClientesConsulta({ volver, registrar, user }) {
  const [texto, setTexto] = useState(''); const [busqueda, setBusqueda] = useState('');
  const [pagina, setPagina] = useState(0); const [id, setId] = useState(null);
  const [resultado, setResultado] = useState(null); const [detalle, setDetalle] = useState(null);
  const [error, setError] = useState(''); const [cargando, setCargando] = useState(true);
  const [intento, setIntento] = useState(0); const [talleres, setTalleres] = useState([]); const [tallerId, setTallerId] = useState('');
  const [orden, setOrden] = useState('nombreCompleto'); const [direccion, setDireccion] = useState('asc'); const admin = user?.rol === 'ADMINISTRADOR_SISTEMA';
  const [editando, setEditando] = useState(false); const [edicion, setEdicion] = useState({}); const [guardando, setGuardando] = useState(false); const [confirmarSuspension, setConfirmarSuspension] = useState(false); const [aviso, setAviso] = useState(''); const [abrirEnEdicion,setAbrirEnEdicion]=useState(false);
  useEffect(() => { consultar('/api/talleres?pagina=0&tamanio=10').then(x => { setTalleres(x.talleres || []); if (admin && x.talleres?.[0]) setTallerId(String(x.talleres[0].id)); }).catch(() => setTalleres([])); }, [admin]);
  useEffect(() => {
    const controller = new AbortController(); setCargando(true); setError(''); setDetalle(null);
    /** Solicita la página o detalle seleccionado; devuelve Promise<void>; captura errores para la alerta. */
    async function cargar() {
      try {
        const params = new URLSearchParams({ pagina: String(pagina), tamanio: '10', busqueda, orden, direccion }); if (admin && tallerId) params.set('tallerId', tallerId);
        if (admin && !tallerId) return;
        const data = await consultar(id ? `/api/clientes/${id}?${params}` : `/api/clientes?${params}`, controller.signal);
        if (!controller.signal.aborted) { if (id) setDetalle(data); else setResultado(data); }
      } catch (exception) { if (!controller.signal.aborted) setError(exception.message); }
      finally { if (!controller.signal.aborted) setCargando(false); }
    }
    cargar(); return () => controller.abort();
  }, [pagina, busqueda, id, intento, tallerId, orden, direccion, admin]);
  /** Aplica búsqueda y vuelve a primera página; recibe submit; devuelve void. */
  function buscar(event) { event.preventDefault(); setPagina(0); setBusqueda(texto.trim()); }
  /** Abre el formulario con los datos actuales del expediente. @returns {void}. */
  function abrirEdicion(){setEdicion({...detalle,tallerId:String(detalle.tallerId)});setEditando(true);setAviso('');}
  /** Abre la ficha o su edición sin cambiar la autorización del servidor. @param {number} clienteId identificador del cliente. @param {boolean} editar indica si debe mostrarse el formulario. @returns {void}. */
  function abrirCliente(clienteId,editar=false){setAbrirEnEdicion(editar);setId(clienteId);}
  /** Activa edición una vez cargada la ficha solicitada desde la acción de lápiz. @returns {void}. */
  useEffect(()=>{if(detalle&&abrirEnEdicion){setEdicion({...detalle,tallerId:String(detalle.tallerId)});setEditando(true);setAviso('');setAbrirEnEdicion(false);}},[detalle,abrirEnEdicion]);
  /** Guarda edición multipart e impide reenvíos mientras la API responde. @param {Event} event envío. @returns {Promise<void>}. */
  async function guardarEdicion(event){event.preventDefault();setGuardando(true);setAviso('');try{const d=new FormData();const datos={...edicion,edad:Number(edicion.edad),tallerId:admin?Number(edicion.tallerId):null,direccion:{calle:edicion.calle,colonia:edicion.colonia,municipio:edicion.municipio,estado:edicion.estado,codigoPostal:edicion.codigoPostal}};d.append('datos',new Blob([JSON.stringify(datos)],{type:'application/json'}));const respuesta=await fetch(`/api/clientes/${id}${admin?`?tallerId=${tallerId}`:''}`,{method:'PUT',headers:{Authorization:`Bearer ${localStorage.getItem('taller_token')}`,'Idempotency-Key':crypto.randomUUID()},body:d});const cuerpo=await respuesta.json();if(!respuesta.ok)throw Error(cuerpo.message||'No fue posible actualizar el cliente.');setAviso('Cliente actualizado');setEditando(false);setIntento(x=>x+1);}catch(e){setAviso(e.message);}finally{setGuardando(false);}}
  /** Suspende con confirmación explícita; la operación del servidor es idempotente. @returns {Promise<void>}. */
  async function suspender(){setGuardando(true);try{const respuesta=await fetch(`/api/clientes/${id}/suspender${admin?`?tallerId=${tallerId}`:''}`,{method:'POST',headers:{Authorization:`Bearer ${localStorage.getItem('taller_token')}`}});const cuerpo=await respuesta.json();if(!respuesta.ok)throw Error(cuerpo.message||'No se pudo suspender el cliente.');setAviso(cuerpo.mensaje);setConfirmarSuspension(false);setIntento(x=>x+1);}catch(e){setAviso(e.message);}finally{setGuardando(false);}}
  /** Reactiva un expediente suspendido sin crear otro registro. @returns {Promise<void>}. */
  async function reactivar(){setGuardando(true);try{const respuesta=await fetch(`/api/clientes/${id}/reactivar${admin?`?tallerId=${tallerId}`:''}`,{method:'POST',headers:{Authorization:`Bearer ${localStorage.getItem('taller_token')}`}});const cuerpo=await respuesta.json();if(!respuesta.ok)throw Error(cuerpo.message||'No se pudo reactivar el cliente.');setAviso(cuerpo.mensaje);setIntento(x=>x+1);}catch(e){setAviso(e.message);}finally{setGuardando(false);}}
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
      </form>{admin && <div className="form-field"><label htmlFor="filtro-taller">Taller</label><select id="filtro-taller" value={tallerId} onChange={e => { setPagina(0); setTallerId(e.target.value); }}>{talleres.map(t => <option key={t.id} value={t.id}>{t.nombre}</option>)}</select></div>}<p className="consulta-muted consulta-note">Ordena el directorio por nombre, estatus o taller.</p></>}
      {error && <div className="notice notice-error" role="alert">{error}<button className="text-button" onClick={() => setIntento(intento + 1)}>Reintentar</button></div>}
      {cargando && <p role="status" className="consulta-loading"><span className="spinner" aria-hidden="true" />Cargando clientes…</p>}
      {!cargando && !error && !id && resultado && <>
        {resultado.clientes.length === 0 ? <div className="consulta-empty" role="status"><Users size={32} /><h2>{busqueda ? 'No hay clientes que coincidan con tu búsqueda' : 'Aún no hay clientes registrados'}</h2><p>Registra un cliente o prueba otra búsqueda.</p></div> :
          <table className="clientes-table"><caption className="sr-only">Clientes registrados, página {pagina + 1}</caption>
            <thead><tr><th scope="col">Cliente <button className="text-button button-small" aria-label="Ordenar por cliente" onClick={()=>{setOrden('nombreCompleto');setDireccion(direccion==='asc'?'desc':'asc')}}>↑↓</button></th><th>Teléfono personal</th><th>E-mail</th><th>Municipio</th><th>Estatus <button className="text-button button-small" aria-label="Ordenar por estatus" onClick={()=>{setOrden('estatus');setDireccion(direccion==='asc'?'desc':'asc')}}>↑↓</button></th><th>Taller</th><th>Acciones</th></tr></thead>
            <tbody>{resultado.clientes.map(cliente => <tr key={cliente.id}>
              <td data-label="Cliente"><div className="cliente-identity"><FotoCliente id={cliente.id} nombre={cliente.nombreCompleto} tallerId={admin?tallerId:null} /><strong>{cliente.nombreCompleto}</strong></div></td>
              <td data-label="Teléfono personal">{cliente.telefonoPersonal}</td><td data-label="E-mail">{cliente.email}</td><td data-label="Municipio">{cliente.municipio}</td>
              <td data-label="Estatus"><span className="consulta-badge">{cliente.estatus==='SUSPENDIDO'?'Suspendido':'Activo'}</span></td><td>{cliente.taller}</td>
              <td data-label="Acciones"><div className="client-action-group"><button type="button" className="icon-button row-action" title="Ver detalle" aria-label={`Ver detalle de ${cliente.nombreCompleto}`} onClick={() => abrirCliente(cliente.id)}><Eye size={18}/><span className="action-label">Ver</span></button><button type="button" className="icon-button row-action" title="Editar cliente" aria-label={`Editar ${cliente.nombreCompleto}`} onClick={() => abrirCliente(cliente.id,true)}><Pencil size={18}/><span className="action-label">Editar</span></button></div></td>
            </tr>)}</tbody></table>}
        <nav className="consulta-pagination" aria-label="Paginación de clientes"><span>{resultado.total} clientes · Página {pagina + 1} de {Math.max(1, resultado.paginas)}</span>
          <div><button className="text-button" disabled={pagina === 0} onClick={() => setPagina(pagina - 1)}>Anterior</button>
          <button className="text-button" disabled={pagina + 1 >= resultado.paginas} onClick={() => setPagina(pagina + 1)}>Siguiente</button></div></nav>
      </>}
      {!cargando && !error && detalle && <><div className="detalle-identity"><FotoCliente id={detalle.id} nombre={detalle.nombreCompleto} tallerId={admin?tallerId:null} /><h2>{detalle.nombreCompleto}</h2><span className="consulta-badge">{detalle.estatus==='SUSPENDIDO'?'Suspendido':'Activo'}</span></div>{aviso&&<div className="notice notice-success" role="status">{aviso}</div>}
        {!editando && <div className="consulta-actions"><button className="primary-button inline-button" onClick={abrirEdicion}>Editar datos</button>{admin&&detalle.estatus!=='SUSPENDIDO'&&<button className="text-button button-danger" onClick={()=>setConfirmarSuspension(true)}>Suspender cliente</button>}{admin&&detalle.estatus==='SUSPENDIDO'&&<button className="primary-button inline-button" disabled={guardando} onClick={reactivar}>Reactivar cliente</button>}</div>}
        {editando&&<form className="field-grid" onSubmit={guardarEdicion}>{[['nombreCompleto','Nombre completo'],['contactoAlternativo','Contacto alternativo'],['edad','Edad'],['fechaNacimiento','Fecha de nacimiento'],['telefonoPersonal','Teléfono personal'],['telefonoTrabajo','Teléfono de trabajo'],['email','E-mail'],['emailTrabajo','E-mail de trabajo'],['calle','Calle'],['colonia','Colonia'],['municipio','Municipio'],['estado','Estado'],['codigoPostal','Código postal']].map(([campo,label])=><div className="form-field" key={campo}><label htmlFor={`editar-${campo}`}>{label}</label><input id={`editar-${campo}`} required={campo!=='emailTrabajo'} type={campo==='fechaNacimiento'?'date':campo==='edad'?'number':campo.includes('email')?'email':'text'} value={edicion[campo]||''} onChange={e=>setEdicion({...edicion,[campo]:e.target.value})}/></div>)}{admin&&<div className="form-field"><label htmlFor="editar-taller">Taller</label><select id="editar-taller" value={edicion.tallerId||''} onChange={e=>setEdicion({...edicion,tallerId:e.target.value})}>{talleres.map(t=><option key={t.id} value={t.id}>{t.nombre}</option>)}</select></div>}<div className="consulta-actions"><button className="primary-button" disabled={guardando}>{guardando?'Guardando…':'Guardar cambios'}</button><button type="button" className="text-button" onClick={()=>setEditando(false)}>Cancelar</button></div></form>}
        <dl className="detalle-grid">{[
          ['Nombre completo', detalle.nombreCompleto], ['Contacto alternativo', detalle.contactoAlternativo],
          ['Edad registrada', detalle.edad], ['Fecha de nacimiento', detalle.fechaNacimiento],
          ['Teléfono personal', detalle.telefonoPersonal], ['Teléfono de trabajo', detalle.telefonoTrabajo],
          ['E-mail', detalle.email], ...(detalle.emailTrabajo ? [['E-mail de trabajo', detalle.emailTrabajo]] : []),
          ['Calle', detalle.calle], ['Colonia', detalle.colonia], ['Municipio', detalle.municipio],
          ['Estado', detalle.estado], ['Código postal', detalle.codigoPostal],
        ].map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl>{confirmarSuspension&&<div className="notice notice-error" role="alertdialog" aria-modal="true"><p>¿Confirmas suspender a este cliente? El expediente seguirá guardado.</p><button className="primary-button button-danger" disabled={guardando} aria-busy={guardando} onClick={suspender}>{guardando?<><span className="spinner" aria-hidden="true"/>Suspendiendo…</>:'Confirmar suspensión'}</button><button className="text-button" disabled={guardando} onClick={()=>setConfirmarSuspension(false)}>Cancelar</button></div>}</>}
    </section>
  </div></main>;
}
