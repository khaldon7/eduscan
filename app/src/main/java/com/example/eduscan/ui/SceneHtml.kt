package com.example.eduscan.ui

import com.example.eduscan.model.Scene
import kotlinx.serialization.json.Json

/** يحوّل وصف المشهد ومكتبة Three.js المضمّنة إلى صفحة تُعرض داخل WebView بدون إنترنت. */
object SceneHtml {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun build(scene: Scene, threeSource: String): String {
        val data = json.encodeToString(Scene.serializer(), scene).replace("</", "<\\/")
        val three = threeSource.replace("</script", "<\\/script")
        return TEMPLATE.replace("__SCENE__", data).replace("__THREE__", three)
    }

    private const val TEMPLATE = """
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<style>
html,body{margin:0;height:100%;overflow:hidden;background:transparent;font-family:sans-serif}
#info{position:absolute;left:8px;right:8px;bottom:8px;background:rgba(255,255,255,.95);color:#222;padding:10px 14px;border-radius:14px;box-shadow:0 2px 10px rgba(0,0,0,.15);font-size:14px}
#info b{font-size:16px}
#msg{position:absolute;top:40%;width:100%;text-align:center;color:#888}
@media (prefers-color-scheme: dark){#info{background:rgba(40,40,46,.95);color:#eee}}
</style>
</head>
<body>
<div id="msg">جارٍ تحميل المشهد...</div>
<div id="info"><b>اضغط على أي عنصر</b></div>
<script>__THREE__</script>
<script>
var SCENE = __SCENE__;
function init() {
  document.getElementById("msg").remove();
  var W = innerWidth, H = innerHeight;
  var renderer = new THREE.WebGLRenderer({antialias: true, alpha: true});
  renderer.setPixelRatio(devicePixelRatio);
  renderer.setSize(W, H);
  document.body.insertBefore(renderer.domElement, document.getElementById("info"));
  var scene = new THREE.Scene();
  var cam = new THREE.PerspectiveCamera(40, W / H, 0.1, 200);
  scene.add(new THREE.AmbientLight(0xffffff, 0.6));
  var dl = new THREE.DirectionalLight(0xffffff, 0.8);
  dl.position.set(3, 5, 4);
  scene.add(dl);
  var group = new THREE.Group();
  scene.add(group);

  var objs = SCENE.objects || [];
  var meshes = [];
  objs.forEach(function (o, i) {
    var s = o.size || [0.5, 1, 1], g;
    if (o.type === "box") g = new THREE.BoxGeometry(s[0], s[1], s[2]);
    else if (o.type === "cylinder") g = new THREE.CylinderGeometry(s[0], s[0], s[1], 24);
    else if (o.type === "torus") g = new THREE.TorusGeometry(s[0], s[1] || 0.2, 12, 32);
    else g = new THREE.SphereGeometry(s[0] || 0.5, 24, 16);
    var m = new THREE.Mesh(g, new THREE.MeshPhongMaterial({color: o.color || "#888888", shininess: 50}));
    var p = o.pos || [0, 0, 0];
    m.position.set(p[0], p[1], p[2]);
    m.userData.i = i;
    group.add(m);
    meshes.push(m);
  });

  (SCENE.connections || []).forEach(function (c) {
    var a = objs[c.from], b = objs[c.to];
    if (!a || !b) return;
    var A = new THREE.Vector3().fromArray(a.pos || [0, 0, 0]);
    var B = new THREE.Vector3().fromArray(b.pos || [0, 0, 0]);
    var d = B.clone().sub(A), len = d.length();
    if (len === 0) return;
    var l = new THREE.Mesh(new THREE.CylinderGeometry(0.05, 0.05, len, 8),
      new THREE.MeshPhongMaterial({color: 0x9aa0a6}));
    l.position.copy(A).add(B).multiplyScalar(0.5);
    l.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), d.normalize());
    group.add(l);
  });

  var box = new THREE.Box3().setFromObject(group);
  var center = box.getCenter(new THREE.Vector3());
  var size = box.getSize(new THREE.Vector3());
  group.position.sub(center);
  var dim = Math.max(size.x, size.y, size.z, 1);
  cam.position.set(0, 0, dim * 1.9 + 1.5);

  var info = document.getElementById("info");
  var ray = new THREE.Raycaster(), ptr = new THREE.Vector2(), down = null;
  var anim = SCENE.animation || "rotate";
  var t0 = Date.now();

  function show(i) {
    var o = objs[i];
    if (!o) return;
    info.innerHTML = "<b></b><div></div>";
    info.querySelector("b").textContent = o.label || "";
    info.querySelector("div").textContent = o.explanation || "";
  }

  var canvas = renderer.domElement;
  canvas.addEventListener("pointerdown", function (e) { down = {x: e.clientX, y: e.clientY}; });
  canvas.addEventListener("pointermove", function (e) {
    if (down && e.buttons) group.rotation.y += e.movementX * 0.01;
  });
  canvas.addEventListener("pointerup", function (e) {
    if (!down) return;
    if (Math.hypot(e.clientX - down.x, e.clientY - down.y) < 8) {
      var r = canvas.getBoundingClientRect();
      ptr.x = ((e.clientX - r.left) / r.width) * 2 - 1;
      ptr.y = -((e.clientY - r.top) / r.height) * 2 + 1;
      ray.setFromCamera(ptr, cam);
      var h = ray.intersectObjects(meshes, false);
      if (h.length) show(h[0].object.userData.i);
    }
    down = null;
  });
  addEventListener("resize", function () {
    W = innerWidth; H = innerHeight;
    renderer.setSize(W, H);
    cam.aspect = W / H;
    cam.updateProjectionMatrix();
  });

  function frame() {
    var t = (Date.now() - t0) / 1000;
    if (anim === "rotate" && !down) group.rotation.y += 0.006;
    if (anim === "pulse") {
      var k = 1 + 0.06 * Math.sin(t * 3);
      meshes.forEach(function (m) { m.scale.set(k, k, k); });
    }
    renderer.render(scene, cam);
    requestAnimationFrame(frame);
  }
  console.log("SCENE_OK " + meshes.length);
  frame();
}
if (typeof THREE === "undefined") {
  document.getElementById("msg").textContent = "تعذّر تحميل مكتبة الرسم داخل التطبيق.";
} else {
  try {
    init();
  } catch (e) {
    document.getElementById("msg").textContent = "خطأ في الرسم: " + e.message;
  }
}
</script>
</body>
</html>
"""
}
