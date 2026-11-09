package com.example.eduscan.ui

object HeartModel {
    val HTML: String = """
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>
html,body{margin:0;height:100%;background:#f5f4ef;font-family:sans-serif;overflow:hidden}
#info{position:absolute;bottom:0;left:0;right:0;background:#fff;padding:12px 16px;border-radius:16px 16px 0 0;box-shadow:0 -2px 12px rgba(0,0,0,.12);font-size:15px}
#info b{font-size:17px}
</style>
</head>
<body>
<div id="info"><b>اختر جزءاً من القلب</b><div id="t"></div></div>
<script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js"></script>
<script>
var D={
 lv:["البطين الأيسر","يضخ الدم المؤكسج إلى كل أنحاء الجسم بضغط عالٍ، ولذلك جداره الأسمك."],
 rv:["البطين الأيمن","يضخ الدم غير المؤكسج إلى الرئتين عبر الشريان الرئوي."],
 la:["الأذين الأيسر","يستقبل الدم المؤكسج العائد من الرئتين ثم يمرره إلى البطين الأيسر."],
 ra:["الأذين الأيمن","يستقبل الدم غير المؤكسج من الجسم عبر الوريد الأجوف."],
 ao:["الأورطي","أكبر شريان في الجسم، يوزع الدم المؤكسج على كل الأعضاء."],
 pa:["الشريان الرئوي","ينقل الدم غير المؤكسج من القلب إلى الرئتين."],
 vc:["الوريد الأجوف","يجمع الدم غير المؤكسج من الجسم ويصبّه في الأذين الأيمن."]
};
var scene=new THREE.Scene();
var cam=new THREE.PerspectiveCamera(40,innerWidth/innerHeight,0.1,100);
cam.position.set(0,0.3,6.5);
var rend=new THREE.WebGLRenderer({antialias:true,alpha:true});
rend.setPixelRatio(devicePixelRatio);
rend.setSize(innerWidth,innerHeight);
document.body.appendChild(rend.domElement);
scene.add(new THREE.AmbientLight(0xffffff,0.6));
var dl=new THREE.DirectionalLight(0xffffff,0.8);
dl.position.set(2,3,5);
scene.add(dl);
var heart=new THREE.Group();
heart.rotation.z=-0.4;
scene.add(heart);

var D0=0.62,NU=64,NV=24,clickable=[];
function surf(u,v){
 var g=Math.pow(Math.max(0,1-v*v),0.4),s=Math.sin(u);
 var x=16*s*s*s/17,y=(13*Math.cos(u)-5*Math.cos(2*u)-2*Math.cos(3*u)-Math.cos(4*u))/17;
 return [x*g,y*g,D0*v];
}
var P=[];
for(var i=0;i<=NV;i++){var v=-1+2*i/NV;for(var j=0;j<NU;j++)P.push(surf(2*Math.PI*j/NU,v));}
var REG=[
 {k:"lv",c:[0.3,-0.4],col:[0.86,0.14,0.12],a:0.06,ph:0},
 {k:"rv",c:[-0.3,-0.35],col:[0.8,0.22,0.2],a:0.06,ph:0.1},
 {k:"la",c:[0.4,0.3],col:[0.92,0.3,0.26],a:0.04,ph:0.3},
 {k:"ra",c:[-0.4,0.3],col:[0.75,0.25,0.22],a:0.04,ph:0.45}
];
var W=P.map(function(p){
 var w=REG.map(function(rg){var dx=p[0]-rg.c[0],dy=p[1]-rg.c[1];return Math.exp(-(dx*dx+dy*dy)/0.05);});
 var s=w.reduce(function(a,b){return a+b;},0)||1;
 return w.map(function(x){return x/s;});
});
var cols=new Float32Array(P.length*3);
P.forEach(function(p,n){REG.forEach(function(rg,k){for(var c=0;c<3;c++)cols[n*3+c]+=W[n][k]*rg.col[c];});});
var idx=[];
for(i=0;i<NV;i++)for(j=0;j<NU;j++){
 var a=i*NU+j,b=i*NU+(j+1)%NU,c2=(i+1)*NU+(j+1)%NU,d=(i+1)*NU+j;
 idx.push(a,b,d,b,c2,d);
}
var geo=new THREE.BufferGeometry();
var pos=new Float32Array(P.length*3);
P.forEach(function(p,n){pos[n*3]=p[0];pos[n*3+1]=p[1];pos[n*3+2]=p[2];});
geo.setAttribute("position",new THREE.BufferAttribute(pos,3));
geo.setAttribute("color",new THREE.BufferAttribute(cols,3));
geo.setIndex(idx);
geo.computeVertexNormals();
var body=new THREE.Mesh(geo,new THREE.MeshPhongMaterial({vertexColors:true,shininess:35,side:THREE.DoubleSide}));
body.userData.k="body";
heart.add(body);
clickable.push(body);

function tube(k,pts,rad,col){
 var cv=new THREE.CatmullRomCurve3(pts.map(function(p){return new THREE.Vector3(p[0],p[1],p[2]);}));
 var m=new THREE.Mesh(new THREE.TubeGeometry(cv,60,rad,16,false),new THREE.MeshPhongMaterial({color:col,shininess:40}));
 m.userData.k=k;
 heart.add(m);
 clickable.push(m);
}
tube("ao",[[0.12,0.5,0.1],[0.2,0.95,0.15],[0.35,1.35,0],[0.1,1.6,-0.3],[-0.15,1.45,-0.45]],0.08,0xE24B4A);
tube("pa",[[-0.12,0.5,0.1],[-0.3,0.95,0.2],[-0.5,1.2,0.1],[-0.55,1.5,0]],0.07,0x378ADD);
tube("vc",[[-0.55,0.55,0.05],[-0.62,1.1,0],[-0.6,1.6,0]],0.09,0x378ADD);

var ray=new THREE.Raycaster(),ptr=new THREE.Vector2(),sel=null,auto=true,down=null;
var REGS={};REG.forEach(function(rg){REGS[rg.k]=rg;});
function show(k){
 var d=D[k]||["اختر جزءاً من القلب","اضغط على أي جزء لمعرفة وظيفته."];
 document.getElementById("t").innerHTML="<div>"+d[0]+"</div><div style='color:#666;font-size:14px;margin-top:4px'>"+d[1]+"</div>";
}
function regionOf(vi){
 var w=W[vi],b=0;
 for(var k=1;k<REG.length;k++)if(w[k]>w[b])b=k;
 return REG[b].k;
}
var el=rend.domElement;
el.addEventListener("pointerdown",function(e){down={x:e.clientX,y:e.clientY};auto=false;});
el.addEventListener("pointermove",function(e){if(down&&e.buttons)heart.rotation.y+=e.movementX*0.01;});
el.addEventListener("pointerup",function(e){
 if(!down)return;
 if(Math.hypot(e.clientX-down.x,e.clientY-down.y)<8){
  var rc=el.getBoundingClientRect();
  ptr.x=((e.clientX-rc.left)/rc.width)*2-1;
  ptr.y=-((e.clientY-rc.top)/rc.height)*2+1;
  ray.setFromCamera(ptr,cam);
  var h=ray.intersectObjects(clickable,false);
  if(h.length){
   var o=h[0].object;
   if(o===body){sel=regionOf(idx[h[0].faceIndex*3]);}else{sel=o.userData.k;}
   show(sel);
  }
 }
 down=null;
});
addEventListener("resize",function(){
 rend.setSize(innerWidth,innerHeight);
 cam.aspect=innerWidth/innerHeight;
 cam.updateProjectionMatrix();
});

function frame(now){
 var t=now/1000;
 if(auto)heart.rotation.y+=0.004;
 REG.forEach(function(rg){
  var amp=sel===rg.k?rg.a*2.3:rg.a;
  var p=Math.pow(Math.max(0,Math.sin(2*Math.PI*t*1.1+rg.ph*2*Math.PI)),6);
  rg.s=1-amp*p;
 });
 var arr=geo.attributes.position.array;
 P.forEach(function(p,n){
  var x=p[0],y=p[1],z=p[2];
  REG.forEach(function(rg,k){
   var f=W[n][k]*(rg.s-1);
   x+=f*(p[0]-rg.c[0]);y+=f*(p[1]-rg.c[1]);z+=f*p[2];
  });
  arr[n*3]=x;arr[n*3+1]=y;arr[n*3+2]=z;
 });
 geo.attributes.position.needsUpdate=true;
 geo.computeVertexNormals();
 rend.render(scene,cam);
 requestAnimationFrame(frame);
}
show(null);
requestAnimationFrame(frame);
</script>
</body>
</html>
""".trimIndent()
}
