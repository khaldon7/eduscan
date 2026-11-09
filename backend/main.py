import base64
import json
import os
import uuid

import requests
from fastapi import FastAPI, HTTPException
from google import genai
from google.genai import types
from pydantic import BaseModel

GEMINI_MODELS = [m.strip() for m in os.getenv(
    "GEMINI_MODELS",
    "gemini-2.5-flash,gemini-2.0-flash,gemini-flash-latest"
).split(",") if m.strip()]

# نماذج OpenRouter المجانية التي تقرأ الصور، تُجرَّب بالترتيب
OPENROUTER_MODELS = [m.strip() for m in os.getenv(
    "OPENROUTER_MODELS",
    "google/gemma-3-27b-it:free,meta-llama/llama-4-maverick:free"
).split(",") if m.strip()]

OPENROUTER_KEY = os.getenv("OPENROUTER_API_KEY", "")
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


def try_gemini(image_bytes: bytes, mime_type: str) -> str:
    last = None
    for model in GEMINI_MODELS:
        try:
            response = client.models.generate_content(
                model=model,
                contents=[types.Part.from_bytes(data=image_bytes, mime_type=mime_type), PROMPT],
                config=types.GenerateContentConfig(response_mime_type="application/json"),
            )
            return response.text
        except Exception as e:
            last = e
            continue
    raise RuntimeError(f"Gemini: {last}")


def try_openrouter(image_bytes: bytes, mime_type: str) -> str:
    if not OPENROUTER_KEY:
        raise RuntimeError("OPENROUTER_API_KEY غير مضبوط")
    image_url = f"data:{mime_type};base64,{base64.b64encode(image_bytes).decode()}"
    last = None
    for model in OPENROUTER_MODELS:
        try:
            r = requests.post(
                "https://openrouter.ai/api/v1/chat/completions",
                headers={"Authorization": f"Bearer {OPENROUTER_KEY}"},
                json={
                    "model": model,
                    "messages": [{
                        "role": "user",
                        "content": [
                            {"type": "image_url", "image_url": {"url": image_url}},
                            {"type": "text", "text": PROMPT},
                        ],
                    }],
                },
                timeout=120,
            )
            if r.status_code != 200:
                last = f"{model}: {r.status_code} {r.text[:200]}"
                continue
            return r.json()["choices"][0]["message"]["content"]
        except Exception as e:
            last = f"{model}: {e}"
            continue
    raise RuntimeError(f"OpenRouter: {last}")


@app.post("/analyze")
def analyze(req: AnalyzeRequest):
    image_bytes = base64.b64decode(req.imageBase64)
    errors = []
    text = None
    for provider in (try_gemini, try_openrouter):
        try:
            text = provider(image_bytes, req.mimeType)
            break
        except Exception as e:
            errors.append(str(e))
    if text is None:
        raise HTTPException(status_code=503, detail=" | ".join(errors))
    try:
        data = parse_json(text)
    except (json.JSONDecodeError, TypeError):
        raise HTTPException(status_code=502, detail="الرد ليس JSON صالحاً")
    data.setdefault("scene", None)
    data["id"] = str(uuid.uuid4())
    return data
