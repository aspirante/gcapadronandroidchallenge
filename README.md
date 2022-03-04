# gcapadronandroidchallenge
Android Technical Challenge Arturo Padrón 
====================
▪ El proyecto a crear será almacenado en el repositorio, puedes hacer uno o varios 
commits en el transcurso del desarrollo del examen. 

REQUERIMIENTOS FUNCIONALES 
====================
 1. PANTALLA PRINCIPAL CON DOS VISTAS: 
 
  ➤ La primera es un Mapa: 
  
    ▪ Utilizando Google Maps SDK 
    ▪ Debe tener un botón que al ser presionado empiece a grabar la ruta por la cual el 
    usuario está caminando. Esa ruta debe ser mostrada en el mapa 
    ▪ Al presionar el botón de grabar nuevamente debe guardar el registro con un 
    nombre que el usuario introduzca. 
    ▪ La ruta debe persistirse localmente. 
    ▪ Debe poner un marcador diferente para el inicio y fin de la ruta. 

  ➤ La segunda es un listado: 
  
    ▪ Debe contener los kilómetros recorrido previamente guardados.  
    ▪ Debe mostrar el nombre de las rutas. 
    ▪ Al dar click sobre algún elemento debe enviar a la pantalla de detalles. 

2. LA PANTALLA DE DETALLES: 
 
  ➤ La vista debe mostrar:  
  
    ▪ La ruta (mapa, imagen, polyline)
    ▪ Distancia recorrida  
    ▪ Tiempo del recorrido 
    ▪ Un botón de compartir (FB, WS, SMS)  
    ▪ Un botón para borrar el registro 
 
REQUERIMIENTOS NO FUNCIONALES
=====================
  1. Utilizar lenguaje kotlin 
  2. Compatibilidad con 4.4 
  3. Usar MVP o MVVM preferentemente. 
  4. Orientación portrait 

PREGUNTAS FRECUENTES
====================
  1. La ruta - ¿Debe mostrarse en un mapa? 
   Sí debe mostrarse en el mapa, que se muestre en tiempo real el mapa y tu ubicación, 
  (durante el recorrido) 
  2. Distancia recorrida - ¿Debe mostrarse en kilómetros o metros? 
  Mostrar kilómetros con 3 decimales 
  3. Un botón de compartir: 
  El objetivo es que se pueda compartir la información en distintas aplicaciones. El share 
  que diga la distancia recorrida, con Km con 3 decimales y el tiempo que duro el recorrido. 

 
RECOMENDACIONES
====================
  1. El examen tiene una duración promedio de 6 horas para un ritmo de trabajo moderado. 
  2. Se recomiendo hacer el examen en una sola sesión y actualizar el repositorio al final. 


 
