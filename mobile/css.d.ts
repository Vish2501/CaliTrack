// Lets TypeScript accept `import "./global.css"` (NativeWind's stylesheet). TypeScript 6 reports
// side-effect imports it can't resolve, and Metro, not TypeScript, handles the CSS.
declare module "*.css";
