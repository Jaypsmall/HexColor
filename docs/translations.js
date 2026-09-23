const translations = {

  es: {

    "hero.subtitle":
      "Paletas de colores, extracción mediante cámara, exploración HSL y herramientas de accesibilidad para desarrolladores, diseñadores y creadores visuales.",

    "buttons.download":
      "↓ Descargar APK",

    "buttons.github":
      "Ver en GitHub",


    "workspace.eyebrow":
      "ESPACIO DE COLOR",

    "workspace.title":
      "Todo lo que necesitas para trabajar con color.",

    "workspace.description":
      "Explora, captura, genera y organiza colores desde una única aplicación Android con una interfaz oscura y detalles dorados.",


    "features.eyebrow":
      "FUNCIONES",

    "features.title":
      "Un kit completo de herramientas de color",


    "features.hsl.title":
      "Rueda HSL dinámica",

    "features.hsl.description":
      "Explora visualmente el espectro de color con el selector Moon Mode y ajusta luces y sombras con precisión.",


    "features.camera.title":
      "Sniper & Cámara",

    "features.camera.description":
      "Extrae colores del mundo real directamente mediante la cámara en modo ventana o pantalla completa.",


    "features.harmonies.title":
      "Armonías de color",

    "features.harmonies.description":
      "Genera automáticamente combinaciones complementarias, análogas y triádicas.",


    "features.wcag.title":
      "Accesibilidad WCAG",

    "features.wcag.description":
      "Comprueba en tiempo real las relaciones de contraste del texto frente a los colores seleccionados.",


    "features.palettes.title":
      "Paletas y favoritos",

    "features.palettes.description":
      "Guarda colores, organiza colecciones y asigna nombres personalizados a tus colores favoritos.",


    "features.export.title":
      "Exportación para desarrolladores",

    "features.export.description":
      "Exporta paletas como variables CSS, arrays JSON o colores XML para Android.",


    "developer.eyebrow":
      "PARA DESARROLLADORES",

    "developer.title":
      "De la paleta al proyecto.",

    "developer.description":
      "HexColor PRO está diseñado para reducir la distancia entre elegir un color y utilizarlo realmente en un proyecto.",

    "developer.formats":
      "FORMATOS DE EXPORTACIÓN",


    "download.title":
      "¿Listo para trabajar con color?",

    "download.description":
      "Descarga HexColor PRO para Android.",

    "download.button":
      "Descargar HexColor PRO",


    "footer.credit":
      "HexColor PRO · Desarrollado con 💛 por Jaypsmall (Architect_D4d)",

    "footer.github":
      "Repositorio de GitHub"

  },


  en: {

    "hero.subtitle":
      "Color palettes, camera extraction, HSL exploration and accessibility tools built for developers, designers and visual creators.",

    "buttons.download":
      "↓ Download APK",

    "buttons.github":
      "View on GitHub",


    "workspace.eyebrow":
      "COLOR WORKSPACE",

    "workspace.title":
      "Everything you need to work with color.",

    "workspace.description":
      "Explore, capture, generate and organize colors from one Android application with a dark interface and gold accents.",


    "features.eyebrow":
      "FEATURES",

    "features.title":
      "A complete color toolkit",


    "features.hsl.title":
      "Dynamic HSL Wheel",

    "features.hsl.description":
      "Explore the color spectrum visually with the Moon Mode picker and fine-tune highlights and shadows.",


    "features.camera.title":
      "Sniper & Camera",

    "features.camera.description":
      "Extract real-world colors directly through the camera in windowed or full-screen mode.",


    "features.harmonies.title":
      "Color Harmonies",

    "features.harmonies.description":
      "Generate complementary, analogous and triadic combinations automatically.",


    "features.wcag.title":
      "WCAG Accessibility",

    "features.wcag.description":
      "Check text contrast ratios in real time against your selected colors.",


    "features.palettes.title":
      "Palettes & Favorites",

    "features.palettes.description":
      "Save colors, organize collections and give your favorite colors custom names.",


    "features.export.title":
      "Developer Export",

    "features.export.description":
      "Export palettes as CSS Variables, JSON arrays or Android XML colors.",


    "developer.eyebrow":
      "FOR DEVELOPERS",

    "developer.title":
      "From palette to project.",

    "developer.description":
      "HexColor PRO is designed to reduce the friction between choosing a color and actually using it in a project.",

    "developer.formats":
      "EXPORT FORMATS",


    "download.title":
      "Ready to work with color?",

    "download.description":
      "Download HexColor PRO for Android.",

    "download.button":
      "Download HexColor PRO",


    "footer.credit":
      "HexColor PRO · Developed with 💛 by Jaypsmall (Architect_D4d)",

    "footer.github":
      "GitHub Repository"

  }

};


function setLanguage(lang) {

  if (!translations[lang]) {
    lang = "en";
  }

  document.documentElement.lang = lang;

  document.querySelectorAll("[data-i18n]").forEach(element => {

    const key = element.getAttribute("data-i18n");

    if (translations[lang][key]) {
      element.textContent = translations[lang][key];
    }

  });


  document.querySelectorAll(".lang-btn").forEach(button => {

    button.classList.toggle(
      "active",
      button.dataset.lang === lang
    );

  });


  localStorage.setItem("hexcolor-language", lang);
}


document.querySelectorAll(".lang-btn").forEach(button => {

  button.addEventListener("click", () => {

    setLanguage(button.dataset.lang);

  });

});


const savedLanguage = localStorage.getItem("hexcolor-language");

const browserLanguage =
  navigator.language.toLowerCase().startsWith("es")
    ? "es"
    : "en";

setLanguage(savedLanguage || browserLanguage);
