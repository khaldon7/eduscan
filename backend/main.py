import base64
import json
import os
import uuid

from fastapi import FastAPI, HTTPException
from google import genai
from google.genai import types
from pydantic import BaseModel

MODEL = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
client = genai.Client(api_key=os.getenv("GEMINI_API_KEY"))
app = FastAPI()


class AnalyzeRequest(BaseModel):
    imageBase64: str
    mimeType: str = "image/jpeg"


PROMPT = """أنت معلم خبير. حلّل صفحة الدرس في الصورة واكتب الرد بلغة الصفحة نفسها.
أعد JSON فقط دون أي نص إضافي، بهذا الشكل بالضبط:
{
  "title": "عنوان الدرس",
  "summary": "ملخص من 2 إلى 3 جمل",
  "sections": [{"heading": "عنوان فرعي", "body": "شرح علمي واضح"}],
  "hotspots": [{"x": 0.0, "y": 0.0, "label": "اسم الجزء", "explanation": "وظيفته وشرحه"}],
  "quiz": [{"question": "سؤال", "options": ["أ", "ب", "ج", "د"], "answerIndex": 0}],
  "scene": {
    "animation": "rotate",
    "objects": [{"type": "sphere", "pos": [0, 0, 0], "size": [0.5, 1, 1], "color": "#E24B4A", "label": "اسم العنصر", "explanation": "شرحه"}],
    "connections": [{"from": 0, "to": 1}]
  }
}
قواعد المشهد ثلاثي الأبعاد:
- أنشئ دائماً مشهداً يمثل الفكرة الرئيسية للدرس (جسم أو جزيء أو جهاز أو تركيب أو مفهوم مكاني)، بين 3 و20 عنصراً.
- type واحد من: sphere أو box أو cylinder أو torus.
- pos و size أرقام تقريبية بين -2 و2. للكرة size = [نصف القطر, 1, 1]، وللأسطوانة size = [نصف القطر, الارتفاع, 1].
- استخدم connections لربط العناصر المتصلة (مثل الروابط الكيميائية أو الأنابيب).
- animation واحد من: rotate أو pulse أو none.
قواعد النقاط الساخنة: x و y نسبيان بين 0 و 1 من عرض الصورة وارتفاعها، وتشير إلى مركز الجزء أو المصطلح المرسوم.
اجعل 3 إلى 6 نقاط ساخنة و3 أسئلة اختبار."""


def parse_json(text: str) -> dict:
    cleaned = text.strip()
    if cleaned.startswith("```"):
        cleaned = cleaned[cleaned.find("{"):cleaned.rfind("}") + 1]
    return json.loads(cleaned)


@app.post("/analyze")
def analyze(req: AnalyzeRequest):
    image_bytes = base64.b64decode(req.imageBase64)
    response = client.models.generate_content(
        model=MODEL,
        contents=[
            types.Part.from_bytes(data=image_bytes, mime_type=req.mimeType),
            PROMPT,
        ],
        config=types.GenerateContentConfig(response_mime_type="application/json"),
    )
    try:
        data = parse_json(response.text)
    except (json.JSONDecodeError, TypeError):
        raise HTTPException(status_code=502, detail="الرد ليس JSON صالحاً")
    data.setdefault("scene", None)
    data["id"] = str(uuid.uuid4())
    return data
