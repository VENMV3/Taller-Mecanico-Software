import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { ArrowLeft, Building2, CalendarDays, CarFront, CheckCircle2, ClipboardList, ContactRound, Eye, EyeOff, ImagePlus, LockKeyhole, LogOut, Mail, MapPin, Phone, ShieldCheck, UserPlus, Users, Wrench } from 'lucide-react';
import './index.css';
import './buttons.css';
import ClientesConsulta from './ClientesConsulta';
import DireccionAutocompletada from './DireccionAutocompletada';
import Talleres from './Talleres';

const API = '/api';
const privilegedRoles = ['ADMINISTRADOR_SISTEMA', 'RECEPCIONISTA'];

/** Renderiza un campo accesible con ayuda y error contextual. @param {object} props propiedades HTML, etiqueta, ayuda y error. @returns {JSX.Element} campo de formulario. */
function Field({ label, id, error, hint, icon: Icon, className = '', children, ...props }) {
  return <div className={`form-field ${className}`}>
    <label htmlFor={id}>{label}</label>
    {children || <div className="input-control">{Icon && <Icon className="input-icon" size={18} aria-hidden="true" />}<input id={id} aria-invalid={Boolean(error)} aria-describedby={`${id}-hint ${error ? `${id}-error` : ''}`} {...props} /></div>}
    {hint && <small id={`${id}-hint`} className="field-hint">{hint}</small>}
    {error && <small id={`${id}-error`} className="field-error" role="alert">{error}</small>}
  </div>;
}

/** Muestra un mensaje accesible de éxito o error. @param {{type:string,children:React.ReactNode}} props contenido del mensaje. @returns {JSX.Element|null} alerta visual. */
function Notice({ type = 'error', children }) {
  if (!children) return null;
  return <div className={`notice notice-${type}`} role={type === 'error' ? 'alert' : 'status'} aria-live="polite">{type === 'success' ? <CheckCircle2 size={19} /> : <ShieldCheck size={19} />}{children}</div>;
}

/** Portal de autenticación y navegación por rol. @returns {JSX.Element} interfaz raíz. */
function App() {
  const [form, setForm] = useState({ email: '', password: '' });
  const [user, setUser] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  /** Actualiza el estado de Login. @param {Event} event evento del campo. @returns {void}. */
  const change = event => setForm({ ...form, [event.target.name]: event.target.value });
  /** Envía las credenciales sin alterar autenticación ni rutas. @param {Event} event evento submit. @returns {Promise<void>}. */
  async function login(event) {
    event.preventDefault(); setError(''); setLoading(true);
    try {
      const response = await fetch(`${API}/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(form) });
      const cuerpo = await response.text(); let data = {};
      try { data = cuerpo ? JSON.parse(cuerpo) : {}; } catch { throw Error('La API devolvió una respuesta inválida. Reinicia el proyecto con ./iniciar-taller.sh.'); }
      if (!response.ok) throw Error(data.message || 'No fue posible validar las credenciales.');
      if (!data.token) throw Error('La API no devolvió una sesión válida. Reinicia el proyecto e inténtalo de nuevo.');
      localStorage.setItem('taller_token', data.token); setUser(data);
    } catch (exception) { setError(exception.message || 'No fue posible iniciar sesión.'); } finally { setLoading(false); }
  }
  if (user) return <Dashboard user={user} logout={() => { localStorage.removeItem('taller_token'); setUser(null); }} />;
  return <main className="auth-page"><section className="auth-card"><aside className="auth-brand"><div className="brand"><span><Wrench size={21} /></span>Taller<span>Core</span></div><div className="brand-copy"><p className="eyebrow">Portal seguro</p><h1>Gestiona cada servicio con confianza.</h1><p>Una experiencia conectada para cada persona de tu taller.</p></div><div className="brand-visual" aria-hidden="true"><div className="visual-ring ring-one"/><div className="visual-ring ring-two"/><div className="visual-center"><Wrench size={42}/></div><span className="visual-chip chip-one">Seguridad</span><span className="visual-chip chip-two">Precisión</span></div><div className="brand-proof"><ShieldCheck size={18} /> Protección de identidad y acceso por rol</div></aside><form className="auth-form" onSubmit={login} noValidate><div className="form-heading"><p className="eyebrow">Acceso al sistema</p><h2>Bienvenido de vuelta</h2><p>Ingresa tus credenciales para continuar.</p></div><Notice>{error}</Notice><Field id="email" label="Correo electrónico" icon={Mail} name="email" type="email" autoComplete="email" placeholder="nombre@taller.com" required value={form.email} onChange={change} error={error ? 'Revisa tus credenciales e intenta de nuevo.' : ''} /><Field id="password" label="Contraseña" className="password-field" error={error ? 'La contraseña no es válida.' : ''}><div className="password-control"><LockKeyhole className="input-icon" size={18} aria-hidden="true"/><input id="password" name="password" type={showPassword ? 'text' : 'password'} autoComplete="current-password" required value={form.password} onChange={change} aria-invalid={Boolean(error)} /><button type="button" className="icon-button" onClick={() => setShowPassword(!showPassword)} aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}>{showPassword ? <EyeOff size={18} /> : <Eye size={18} />}</button></div></Field><button className="primary-button" disabled={loading} aria-busy={loading}>{loading && <span className="spinner" aria-hidden="true"/>}{loading ? 'Validando acceso…' : 'Iniciar sesión'}</button><p className="form-footer">Tu sesión se adapta a los permisos de tu rol.</p></form></section></main>;
}

/** Muestra el panel existente y permite acceso visual al registro autorizado. @param {{user:object,logout:Function}} props identidad autenticada. @returns {JSX.Element} panel. */
function Dashboard({ user, logout }) {
  const [consulta, setConsulta] = useState(false);
  const [registration, setRegistration] = useState(false); const [talleres, setTalleres] = useState(false); const allowed = privilegedRoles.includes(user.rol); const admin = user.rol === 'ADMINISTRADOR_SISTEMA'; const client = user.rol === 'CLIENTE';
  if (registration) return <ClienteRegistro user={user} volver={() => setRegistration(false)} />;
  if (talleres && admin) return <Talleres volver={() => setTalleres(false)} />;
  if (consulta && allowed) return <ClientesConsulta user={user} volver={() => setConsulta(false)} registrar={() => setRegistration(true)} />;
  const cards = client ? [['Mis vehículos', '0', CarFront], ['Próxima cita', 'Sin cita', CalendarDays], ['Servicios', '0', ClipboardList]] : [['Órdenes activas', '0', ClipboardList], ['Vehículos', '0', CarFront], ['Usuarios', '—', Users]];
  return <main className="min-h-screen bg-black"><header className="app-header"><div className="brand"><span><Wrench size={19} /></span>Taller<span>Core</span></div><button onClick={logout} className="text-button"><LogOut size={16} />Salir</button></header><div className="dashboard"><p className="eyebrow">Panel de {user.rol}</p><h1>Hola, {user.nombre.split(' ')[0]}</h1>{allowed && <button onClick={() => setRegistration(true)} className="primary-button inline-button"><UserPlus size={18} />Registrar cliente</button>}{allowed && <button className="text-button" onClick={() => setConsulta(true)}><Users size={18} />Consultar clientes</button>}{admin && <button className="text-button" onClick={() => setTalleres(true)}><Wrench size={18} />Administrar talleres</button>}<div className="card-grid">{cards.map(([title, value, Icon]) => <article key={title} className="metric-card"><Icon className="text-blue-500" /><p>{title}</p><strong>{value}</strong></article>)}</div></div></main>;
}

/** Registra un cliente manteniendo el contrato multipart original. @param {{user:object,volver:Function}} props de sesión y retorno. @returns {JSX.Element} formulario por secciones. */
function ClienteRegistro({ user, volver }) {
  const [values, setValues] = useState({ nombreCompleto: '', contactoAlternativo: '', edad: '', fechaNacimiento: '', telefonoPersonal: '', telefonoTrabajo: '', email: '', emailTrabajo: '', calle: '', colonia: '', municipio: '', estado: '', codigoPostal: '', tallerId: '' }); const [talleres,setTalleres]=useState([]);
  const [photo, setPhoto] = useState(null); const [message, setMessage] = useState(''); const [messageType, setMessageType] = useState('error'); const [saving, setSaving] = useState(false); const [key] = useState(() => crypto.randomUUID());
  const preview = useMemo(() => photo ? URL.createObjectURL(photo) : '', [photo]);
  /** Carga talleres permitidos para selector de administrador. @returns {void}. */ useEffect(()=>{fetch('/api/talleres?pagina=0&tamanio=10',{headers:{Authorization:`Bearer ${localStorage.getItem('taller_token')}`}}).then(r=>r.ok?r.json():null).then(d=>setTalleres(d?.talleres||[])).catch(()=>setTalleres([]));},[]);
  /** Actualiza un campo sin alterar la estructura enviada al backend. @param {Event} event evento de entrada. @returns {void}. */
  const change = event => setValues({ ...values, [event.target.name]: event.target.value });
  /** Valida el archivo desde UI y actualiza la vista previa. @param {Event} event evento de archivo. @returns {void}. */
  function selectPhoto(event) { const file = event.target.files[0]; if (!file) return; if (!file.type.startsWith('image/') || file.size > 15 * 1024 * 1024) { setPhoto(null); setMessageType('error'); setMessage('Selecciona una imagen válida de máximo 15 MB.'); return; } setPhoto(file); setMessage(''); }
  /** Envía el FormData idempotente y conserva validaciones existentes. @param {Event} event evento submit. @returns {Promise<void>}. */
  async function save(event) {
    event.preventDefault(); setMessage(''); if (!photo) { setMessageType('error'); setMessage('Selecciona una fotografía válida.'); return; }
    const birth = new Date(values.fechaNacimiento), now = new Date(), age = now.getFullYear() - birth.getFullYear() - (now < new Date(now.getFullYear(), birth.getMonth(), birth.getDate()) ? 1 : 0); if (Number(values.edad) !== age) { setMessageType('error'); setMessage('La edad no coincide con la fecha de nacimiento.'); return; }
    if(user.rol==='ADMINISTRADOR_SISTEMA'&&!values.tallerId){setMessageType('error');setMessage('Selecciona el taller del cliente.');return;} setSaving(true); const data = new FormData(); data.append('datos', new Blob([JSON.stringify({ ...values, tallerId: values.tallerId?Number(values.tallerId):null, edad: Number(values.edad), direccion: { calle: values.calle, colonia: values.colonia, municipio: values.municipio, estado: values.estado, codigoPostal: values.codigoPostal } })], { type: 'application/json' })); data.append('fotografia', photo);
    try { const response = await fetch(`${API}/clientes`, { method: 'POST', headers: { Authorization: `Bearer ${localStorage.getItem('taller_token')}`, 'Idempotency-Key': key }, body: data }); const result = await response.json(); if (!response.ok) throw Error(result.message); setMessageType('success'); setMessage('Cliente Registrado'); } catch (exception) { setMessageType('error'); setMessage(exception.message || 'No fue posible registrar al cliente.'); } finally { setSaving(false); }
  }
  const personal = [['nombreCompleto', 'Nombre completo', 'text', true], ['contactoAlternativo', 'Nombre de contacto alternativo', 'text', true], ['edad', 'Edad', 'number', true], ['fechaNacimiento', 'Fecha de nacimiento', 'date', true]]; const contact = [['telefonoPersonal', 'Teléfono personal', 'tel', true], ['telefonoTrabajo', 'Teléfono de trabajo', 'tel', true], ['email', 'E-mail', 'email', true], ['emailTrabajo', 'E-mail de trabajo', 'email', false]];
  const renderFields = fields => fields.map(([name, label, type, required]) => <Field key={name} id={name} label={label} icon={name.includes('telefono') ? Phone : type === 'email' ? Mail : name === 'codigoPostal' || ['calle','colonia','municipio','estado'].includes(name) ? MapPin : ContactRound} name={name} type={type} required={required ?? true} value={values[name]} onChange={change} placeholder={type === 'email' ? 'correo@ejemplo.com' : ''} hint={name === 'codigoPostal' ? 'Cinco dígitos.' : ''} />);
  return <main className="registration-page"><div className="registration-shell"><button onClick={volver} className="text-button back-button"><ArrowLeft size={18} />Volver al panel</button><section className="registration-card"><header className="form-heading"><p className="eyebrow">Alta de cliente</p><h1>Registro de Cliente</h1><p>Completa los datos para crear un expediente nuevo.</p><div className="progress-steps" aria-label="Secciones del registro"><span className="active">1 Datos</span><span>2 Contacto</span><span>3 Fotografía</span><span>4 Dirección</span></div></header><Notice type={messageType}>{message}</Notice><form onSubmit={save} noValidate>{user.rol==='ADMINISTRADOR_SISTEMA'&&<fieldset><legend><Building2 size={17}/>Taller</legend><div className="form-field"><label htmlFor="tallerId">Taller asignado</label><select id="tallerId" value={values.tallerId} required onChange={e=>setValues({...values,tallerId:e.target.value})}><option value="">Selecciona un taller</option>{talleres.map(t=><option key={t.id} value={t.id}>{t.nombre}</option>)}</select></div></fieldset>}<fieldset><legend><ContactRound size={17}/>Datos personales</legend><div className="field-grid">{renderFields(personal)}</div></fieldset><fieldset><legend><Phone size={17}/>Contacto</legend><div className="field-grid">{renderFields(contact)}</div></fieldset><fieldset><legend><ImagePlus size={17}/>Fotografía</legend><div className="photo-zone"><label htmlFor="fotografia" className="photo-picker"><ImagePlus size={26} /><strong>Arrastra tu imagen o selecciónala</strong><small>JPG, PNG u otro formato de imagen válido · máximo 15 MB</small><input id="fotografia" name="fotografia" type="file" accept="image/*" required onChange={selectPhoto} /></label>{preview && <div className="photo-preview-wrap"><img src={preview} className="photo-preview" alt="Vista previa de la fotografía del cliente" /><span>Vista previa</span></div>}</div></fieldset><fieldset><legend><MapPin size={17}/>Dirección</legend><DireccionAutocompletada values={values} setValues={setValues}/></fieldset><button className="primary-button submit-button" disabled={saving} aria-busy={saving}>{saving && <span className="spinner" aria-hidden="true"/>}{saving ? 'Guardando cliente…' : 'Registrar cliente'}</button></form></section></div></main>;
}

createRoot(document.getElementById('root')).render(<App />);
