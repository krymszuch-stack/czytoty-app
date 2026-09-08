// Repo-native vector redraw of the supplied brand reference; no raster poster is packaged in the app.
const fs = require('fs');
const path = require('path');
const { Resvg } = require('../build/brand-tools/node_modules/@resvg/resvg-js');
const root = path.resolve(__dirname, '..');
const out = path.join(root, 'artifacts', 'brand');
const res = path.join(root, 'app/src/main/res');
fs.mkdirSync(out, { recursive: true });
const bubble = 'M47 4 C22 4 4 22 4 43 C4 64 19 82 48 85 C52 85 51 82 47 78 C43 73 40 67 41 65 C42 63 48 64 58 64 C72 64 76 57 84 43 C91 29 95 26 108 27 C105 14 90 4 68 4 Z';
const phone = 'M106 35 C116 35 123 43 123 54 C122 81 106 104 81 112 C72 116 58 116 57 110 C55 105 61 95 67 93 C72 91 77 91 82 88 C89 85 95 79 98 73 L90 69 C84 67 82 64 85 58 L94 42 C97 37 101 34 106 35 Z';
const dot = 'M78 130 A11 11 0 1 1 56 130 A11 11 0 1 1 78 130 Z';
const holes = [39,58,77].map(x => 'M'+(x+6)+' 40 A6 6 0 1 1 '+(x-6)+' 40 A6 6 0 1 1 '+(x+6)+' 40 Z').join(' ');
const gradient = '<linearGradient id="purple" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#E3B7FF"/><stop offset=".55" stop-color="#A875FF"/><stop offset="1" stop-color="#7949EC"/></linearGradient>';
const palettes = {
  dark: ['#0A0A12','url(#purple)','#FAF4FF','#0A0A12'],
  light: ['#F8F2FF','url(#purple)','#211A36','#211A36'],
  mono: ['#8153ED','#FFFFFF','#FFFFFF','#8153ED'],
  bw: ['#FFFFFF','#111111','#111111','#FFFFFF']
};
function shape(a,b,c) { return '<path d="'+bubble+'" fill="'+a+'"/><path d="'+holes+'" fill="'+c+'"/><path d="'+phone+' '+dot+'" fill="'+b+'"/>'; }
function writeSvg(name,svg,sizes) {
 fs.writeFileSync(path.join(out,name+'.svg'),svg);
 for(const size of sizes) fs.writeFileSync(path.join(out,name+'-'+size+'.png'),new Resvg(svg,{fitTo:{mode:'width',value:size}}).render().asPng());
}
for(const [name,[bg,a,b,c]] of Object.entries(palettes)) {
 const svg='<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="1024" viewBox="0 0 160 160"><defs>'+gradient+'</defs><rect x="1" y="1" width="158" height="158" rx="32" fill="'+bg+'" stroke="'+(name==='dark'?'#40305F':bg)+'"/><g transform="translate(27 13) scale(.84)">'+shape(a,b,c)+'</g></svg>';
 writeSvg('icon-'+name,svg,[1024,256,64]);
}
writeSvg('logo-mark','<svg xmlns="http://www.w3.org/2000/svg" width="128" height="144" viewBox="0 0 128 144"><defs>'+gradient+'</defs>'+shape('url(#purple)','#FAF4FF','#0A0A12')+'</svg>',[1024,256,64]);
const gradXml='<aapt:attr name="android:fillColor"><gradient android:startX="10" android:startY="5" android:endX="95" android:endY="88" android:type="linear"><item android:offset="0" android:color="#E3B7FF"/><item android:offset="0.55" android:color="#A875FF"/><item android:offset="1" android:color="#7949EC"/></gradient></aapt:attr>';
function xmlMark(monochrome=false) {
 return '<path android:pathData="'+bubble+(monochrome?' '+holes:'')+'"'+(monochrome?' android:fillType="evenOdd" android:fillColor="#FFFFFF"/>': '>'+gradXml+'</path><path android:pathData="'+holes+'" android:fillColor="#0A0A12"/>')+
 '<path android:pathData="'+phone+' '+dot+'" android:fillColor="#FAF4FF"/>';
}
function vector(width,height,body) {return '<vector xmlns:android="http://schemas.android.com/apk/res/android" xmlns:aapt="http://schemas.android.com/aapt" android:width="'+width+'dp" android:height="'+height+'dp" android:viewportWidth="'+width+'" android:viewportHeight="'+height+'">'+body+'</vector>\n';}
fs.writeFileSync(path.join(res,'drawable/ic_brand_mark.xml'),vector(128,144,xmlMark()));
fs.writeFileSync(path.join(res,'drawable/ic_launcher_foreground.xml'),vector(108,108,'<group android:scaleX="0.44" android:scaleY="0.44" android:translateX="26" android:translateY="22">'+xmlMark()+'</group>'));
fs.writeFileSync(path.join(res,'drawable/ic_launcher_monochrome.xml'),vector(108,108,'<group android:scaleX="0.44" android:scaleY="0.44" android:translateX="26" android:translateY="22">'+xmlMark(true)+'</group>'));
fs.writeFileSync(path.join(res,'drawable/ic_launcher_background.xml'),'<shape xmlns:android="http://schemas.android.com/apk/res/android"><solid android:color="#0A0A12"/></shape>\n');
const adaptive='<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android"><background android:drawable="@drawable/ic_launcher_background"/><foreground android:drawable="@drawable/ic_launcher_foreground"/><monochrome android:drawable="@drawable/ic_launcher_monochrome"/></adaptive-icon>\n';
for(const n of ['ic_launcher','ic_launcher_round']) fs.writeFileSync(path.join(res,'mipmap-anydpi-v26',n+'.xml'),adaptive);
const glyphs = {
  ic_phone_lookup: ['M20.6 16.1 L17.2 13.8 Q16.3 13.3 15.6 14.1 L14 15.7 Q10.2 13.8 8.3 10 L9.9 8.4 Q10.7 7.7 10.2 6.8 L7.9 3.4 Q7.3 2.5 6.3 3 L3.9 4.5 Q2.9 5.2 3.2 6.7 C4.7 13.8 10.2 19.3 17.3 20.8 Q18.8 21.1 19.5 20.1 L21 17.7 Q21.5 16.7 20.6 16.1 Z'],
  ic_messages_sms: ['M5 18 C2.5 16 2 12 3 8.5 C4 5 7 3 11.8 3 C17.1 3 21 6.1 21 11 C21 15.9 17.1 19 12 19 L9 19 L5 22 Z','M7.5 11 L7.51 11 M12 11 L12.01 11 M16.5 11 L16.51 11'],
  ic_protection_shield: ['M12 2.5 Q8 5 3.5 5.5 L3.5 11 C3.5 16 6.9 20 12 22 C17.1 20 20.5 16 20.5 11 L20.5 5.5 Q16 5 12 2.5 Z','M8 12 L11 15 L16 9.5'],
  ic_account: ['M16 7 A4 4 0 1 1 8 7 A4 4 0 1 1 16 7 Z','M4 21 L4 19 C4 15.5 7.6 13 12 13 C16.4 13 20 15.5 20 19 L20 21 Z'],
  ic_search: ['M17 10 A7 7 0 1 1 3 10 A7 7 0 1 1 17 10 Z','M15 15 L21 21']
};
for(const [name,paths] of Object.entries(glyphs)) {
 const svg='<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><g fill="none" stroke="#C49BFF" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">'+paths.map(d=>'<path d="'+d+'"/>').join('')+'</g></svg>';
 writeSvg(name.replace('ic_',''),svg,[96,48,24]);
 fs.writeFileSync(path.join(res,'drawable',name+'.xml'),vector(24,24,paths.map(d=>'<path android:pathData="'+d+'" android:fillColor="#00000000" android:strokeColor="#C49BFF" android:strokeWidth="1.8" android:strokeLineCap="round" android:strokeLineJoin="round"/>').join('')));
}
for (const [name, bg, ink] of [['dark','#0A0A12','#FAF4FF'], ['light','#F8F2FF','#211A36']]) {
 const svg='<svg xmlns="http://www.w3.org/2000/svg" width="720" height="210" viewBox="0 0 720 210"><defs>'+gradient+'</defs><rect width="720" height="210" rx="24" fill="'+bg+'"/><g transform="translate(26 22) scale(1.15)">'+shape('url(#purple)',ink,bg)+'</g><text x="202" y="112" font-family="Segoe UI, sans-serif" font-size="76" font-weight="700" fill="'+ink+'">CzyTo<tspan fill="#A875FF">Ty?</tspan></text><text x="206" y="158" font-family="Segoe UI, sans-serif" font-size="27" fill="'+ink+'">Telefon · SMS · Ochrona</text></svg>';
 writeSvg('logo-'+name,svg,[1440,720]);
}
console.log('Created vector brand and Android adaptive icons in '+out);
