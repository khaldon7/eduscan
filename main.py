import json
import os
import uuid

import anthropic
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

MODEL = os.getenv("CLAUDE_MODEL", "claude-sonnet-5-5")
client = anthropic.Anthropic()  # يقرأ ANTHROPIC_API_KEY من البيئة
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
  "quiz": [{"question": "سؤال", "options": ["أ", "ب", "ج", "د"], "answerIndex": 0}]
}
قواعد النقاط الساخنة: x و y نسبيان بين 0 و 1 من عرض الصورة وارتفاعها، وتشير إلى مركز الجزء أو المصطلح المرسوم.
اجعل 3 إلى 6 نقاط ساخنة و3 أسئلة اختبار."""


def parse_json(text: str) -> dict:
    cleaned = text.strip()
    if cleaned.startswith("```"):
        cleaned = cleaned[cleaned.find("{"):cleaned.rfind("}") + 1]
    return json.loads(cleaned)


@app.post("/analyze")
def analyze(req: AnalyzeRequest):
    msg = client.messages.create(
        model=MODEL,
        max_tokens=4000,
        messages=[{
            "role": "user",
            "content": [
                {"type": "image", "source": {"type": "base64", "media_type": req.mimeType, "data": req.imageBase64}},
                {"type": "text", "text": PROMPT},
            ],
        }],
    )
    text = "".join(block.text for block in msg.content if block.type == "text")
    try:
        data = parse_json(text)
    except json.JSONDecodeError:
        raise HTTPException(status_code=502, detail="الرد ليس JSON صالحاً")
    data["id"] = str(uuid.uuid4())
    return data
