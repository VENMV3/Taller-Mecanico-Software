import React, { useEffect, useState } from 'react';
import { ChevronDown, ChevronUp, ClipboardList } from 'lucide-react';
import { statusApi } from './statusApi';
import './clientes-consulta.css';

const columnasOrdenables = ['id', 'strValor', 'strDescripcion'];

/** Muestra el catálogo de estatus de solo lectura con paginación y orden del servidor. @returns {React.JSX.Element} Vista de estatus. */
export default function StatusListado() {
  const [pagina, setPagina] = useState(0);
  const [orden, setOrden] = useState('id');
  const [direccion, setDireccion] = useState('asc');
  const [resultado, setResultado] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [reintento, setReintento] = useState(0);

  useEffect(() => {
    let vigente = true;
    setCargando(true); setError('');
    statusApi.listar(pagina, 10, orden, direccion)
      .then((datos) => { if (vigente) setResultado(datos); })
      .catch((causa) => { if (vigente) setError(causa.message || 'No se pudo cargar el catálogo de estatus.'); })
      .finally(() => { if (vigente) setCargando(false); });
    return () => { vigente = false; };
  }, [pagina, orden, direccion, reintento]);

  /** Alterna el orden de una columna permitida. @param {string} columna Columna a ordenar. @returns {void} */
  function ordenarPor(columna) {
    if (!columnasOrdenables.includes(columna)) return;
    setPagina(0);
    setDireccion((actual) => (orden === columna && actual === 'asc' ? 'desc' : 'asc'));
    setOrden(columna);
  }

  /** Obtiene el indicador visual del orden actual. @param {string} columna Columna evaluada. @returns {React.JSX.Element|null} Icono o nulo. */
  function indicadorOrden(columna) {
    if (orden !== columna) return null;
    return direccion === 'asc' ? <ChevronUp size={16} aria-hidden="true" /> : <ChevronDown size={16} aria-hidden="true" />;
  }

  const estatus = resultado?.status || [];
  const totalPaginas = resultado?.totalPaginas || 0;
  const total = resultado?.total || 0;
  return <main className="consulta-page"><div className="consulta-shell"><section className="consulta-card" aria-busy={cargando}>
    <header className="consulta-heading"><div><span className="section-kicker"><ClipboardList size={16} aria-hidden="true" /> Catálogo del sistema</span><h1>Estatus</h1><p>Consulta los estados disponibles para la operación del taller.</p></div></header>
    {error && <div className="consulta-notice consulta-error" role="alert"><span>{error}</span><button type="button" className="text-button" onClick={() => setReintento((valor) => valor + 1)}>Reintentar</button></div>}
    {cargando && <p className="consulta-loading" role="status">Cargando estatus…</p>}
    {!cargando && !error && estatus.length === 0 && <div className="consulta-empty" role="status"><ClipboardList size={28} aria-hidden="true" /><p>Aún no hay estatus registrados.</p></div>}
    {!cargando && !error && estatus.length > 0 && <><div className="clientes-table-wrap"><table className="clientes-table"><caption className="sr-only">Listado paginado de estatus</caption><thead><tr>{[['id', 'ID'], ['strValor', 'Valor'], ['strDescripcion', 'Descripción']].map(([columna, etiqueta]) => <th key={columna} scope="col" aria-sort={orden === columna ? (direccion === 'asc' ? 'ascending' : 'descending') : 'none'}><button type="button" className="table-sort-button" onClick={() => ordenarPor(columna)}>{etiqueta} {indicadorOrden(columna)}</button></th>)}</tr></thead><tbody>{estatus.map((estado) => <tr key={estado.id}><td data-label="ID">{estado.id}</td><td data-label="Valor"><span className="status-badge">{estado.strValor}</span></td><td data-label="Descripción">{estado.strDescripcion}</td></tr>)}</tbody></table></div><nav className="consulta-pagination" aria-label="Paginación de estatus"><span>Página {pagina + 1} de {Math.max(totalPaginas, 1)} · {total} estatus</span><div><button type="button" className="text-button" disabled={pagina <= 0} onClick={() => setPagina((actual) => Math.max(0, actual - 1))}>Anterior</button><button type="button" className="text-button" disabled={totalPaginas === 0 || pagina >= totalPaginas - 1} onClick={() => setPagina((actual) => actual + 1)}>Siguiente</button></div></nav></>}
  </section></div></main>;
}
