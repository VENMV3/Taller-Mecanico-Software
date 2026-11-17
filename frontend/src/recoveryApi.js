/** Ejecuta los dos pasos públicos de recuperación sin exponer detalles de cuenta. */
async function post(path, body) { const response=await fetch(`/api/auth/recover/${path}`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)}); if(!response.ok){const data=await response.json().catch(()=>({}));throw Error(data.message||'No fue posible completar la recuperación.');} }
export const solicitarRecuperacion=identificador=>post('request',{identificador});
export const confirmarRecuperacion=datos=>post('confirm',datos);
