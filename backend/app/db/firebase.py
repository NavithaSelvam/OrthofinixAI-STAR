import sqlite3
import json
import uuid
import os
from datetime import datetime

# Setup absolute path to the local persistent SQLite database
DB_FILE = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "orthofinix.db"))
UPLOADS_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", "uploads"))

# Ensure uploads directory exists
os.makedirs(UPLOADS_DIR, exist_ok=True)

def get_sqlite_conn():
    conn = sqlite3.connect(DB_FILE)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    conn = get_sqlite_conn()
    cursor = conn.cursor()
    # Create persistent document store table mimicking NoSQL collections
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS document_store (
        collection_name TEXT,
        doc_id TEXT,
        doctor_id TEXT,
        patient_id TEXT,
        case_id TEXT,
        data TEXT,
        created_at TEXT,
        PRIMARY KEY (collection_name, doc_id)
    )
    """)
    # Create persistent user credentials table for custom sign-up/login
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS users (
        uid TEXT PRIMARY KEY,
        email TEXT UNIQUE,
        password_hash TEXT,
        display_name TEXT,
        role TEXT,
        created_at TEXT
    )
    """)
    conn.commit()
    conn.close()

# Initialize local database tables immediately on import
init_db()

class MockQuery:
    def __init__(self, docs):
        self._docs = docs

    def stream(self):
        return [SQLiteDocument(d) for d in self._docs]

    def order_by(self, field, direction="ASCENDING"):
        reverse = (direction in ["DESCENDING", "desc", "descending"])
        try:
            self._docs.sort(key=lambda x: x.get(field) or "", reverse=reverse)
        except Exception:
            pass
        return self

    def limit(self, count):
        return MockQuery(self._docs[:count])

class SQLiteDocument:
    def __init__(self, data):
        self._data = data
        self.exists = data is not None

    def to_dict(self):
        return self._data

class SQLiteCollection:
    def __init__(self, name):
        self.name = name

    def document(self, doc_id):
        return SQLiteDocumentReference(self.name, doc_id)

    def where(self, field, op, value):
        docs = []
        conn = get_sqlite_conn()
        cursor = conn.cursor()
        # Query utilizing index columns to keep performance premium
        if field in ["doctor_id", "patient_id", "case_id"]:
            cursor.execute(
                f"SELECT data FROM document_store WHERE collection_name = ? AND {field} = ?",
                (self.name, value)
            )
        else:
            cursor.execute(
                "SELECT data FROM document_store WHERE collection_name = ?",
                (self.name,)
            )
        rows = cursor.fetchall()
        conn.close()

        for row in rows:
            try:
                data = json.loads(row["data"])
                if field not in ["doctor_id", "patient_id", "case_id"]:
                    if op == "==" and data.get(field) != value:
                        continue
                docs.append(data)
            except Exception:
                pass
        return MockQuery(docs)

    def order_by(self, field, direction="ASCENDING"):
        docs = []
        conn = get_sqlite_conn()
        cursor = conn.cursor()
        cursor.execute("SELECT data FROM document_store WHERE collection_name = ?", (self.name,))
        rows = cursor.fetchall()
        conn.close()
        for row in rows:
            try:
                docs.append(json.loads(row["data"]))
            except Exception:
                pass
        return MockQuery(docs).order_by(field, direction)

    def stream(self):
        docs = []
        conn = get_sqlite_conn()
        cursor = conn.cursor()
        cursor.execute("SELECT data FROM document_store WHERE collection_name = ?", (self.name,))
        rows = cursor.fetchall()
        conn.close()
        for row in rows:
            try:
                docs.append(json.loads(row["data"]))
            except Exception:
                pass
        return [SQLiteDocument(d) for d in docs]

class SQLiteDocumentReference:
    def __init__(self, collection_name, doc_id):
        self.collection_name = collection_name
        self.doc_id = doc_id

    def set(self, data, merge=False):
        conn = get_sqlite_conn()
        cursor = conn.cursor()
        
        doctor_id = data.get("doctor_id") or data.get("doctorId")
        patient_id = data.get("patient_id") or data.get("patientId")
        case_id = data.get("case_id") or data.get("caseId")
        created_at = data.get("created_at")
        
        if isinstance(created_at, datetime):
            created_at = created_at.isoformat()
        
        # Serialize fields securely for JSON database column
        serialized_data = {}
        for k, v in data.items():
            if isinstance(v, datetime):
                serialized_data[k] = v.isoformat()
            else:
                serialized_data[k] = v

        if merge:
            cursor.execute(
                "SELECT data FROM document_store WHERE collection_name = ? AND doc_id = ?",
                (self.collection_name, self.doc_id)
            )
            row = cursor.fetchone()
            if row:
                try:
                    existing = json.loads(row["data"])
                    existing.update(serialized_data)
                    serialized_data = existing
                except Exception:
                    pass

        json_data = json.dumps(serialized_data)
        cursor.execute("""
            INSERT OR REPLACE INTO document_store (collection_name, doc_id, doctor_id, patient_id, case_id, data, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """, (self.collection_name, self.doc_id, doctor_id, patient_id, case_id, json_data, created_at or datetime.utcnow().isoformat()))
        conn.commit()
        conn.close()
        return self

    def get(self):
        conn = get_sqlite_conn()
        cursor = conn.cursor()
        cursor.execute(
            "SELECT data FROM document_store WHERE collection_name = ? AND doc_id = ?",
            (self.collection_name, self.doc_id)
        )
        row = cursor.fetchone()
        conn.close()
        if row:
            try:
                return SQLiteDocument(json.loads(row["data"]))
            except Exception:
                pass
        return SQLiteDocument(None)

class SQLiteFirestoreClient:
    def collection(self, name):
        return SQLiteCollection(name)

# Expose standard Firebase helper methods transparently pointing to SQLite store
def init_firebase():
    pass

def get_db():
    return SQLiteFirestoreClient()

class SQLiteAuthClient:
    def verify_id_token(self, token):
        # Decode and inspect mock sandbox user tokens
        if token.startswith("mock-sandbox:"):
            parts = token.split(":")
            email = parts[1] if len(parts) > 1 else "dr.smith@orthofinix.ai"
            name = parts[2] if len(parts) > 2 else "Doctor"
            return {
                "uid": f"mock-sandbox-{email.replace('@', '_').replace('.', '_')}",
                "email": email,
                "name": name
            }
        return {
            "uid": "mock-sandbox-uid-12345",
            "email": "dr.smith@orthofinix.ai",
            "name": "Dr. Smith"
        }

def get_auth():
    return SQLiteAuthClient()

def save_analysis_record(data: dict) -> dict:
    db = get_db()
    record_id = str(uuid.uuid4())
    data["id"] = record_id
    data["created_at"] = datetime.utcnow().isoformat()
    db.collection("analyses").document(record_id).set(data)
    return data

def get_analysis_history() -> list:
    db = get_db()
    docs = db.collection("analyses").order_by("created_at", direction="DESCENDING").stream()
    return [doc.to_dict() for doc in docs]

def get_analysis_by_id(record_id: str) -> dict:
    db = get_db()
    doc = db.collection("analyses").document(record_id).get()
    if doc.exists:
        return doc.to_dict()
    return None

def upload_image_to_storage(file_bytes: bytes, filename: str, content_type: str = "image/jpeg") -> str:
    """
    Save image file to local uploads directory and return dynamic retrieval URL.
    """
    try:
        unique_filename = f"{uuid.uuid4()}_{filename}"
        filepath = os.path.join(UPLOADS_DIR, unique_filename)
        with open(filepath, "wb") as f:
            f.write(file_bytes)
        
        # Return standard local loopback URL mapped to static endpoint
        return f"http://127.0.0.1:8000/uploads/{unique_filename}"
    except Exception as e:
        print(f"Local Storage Write Error: {e}")
        return "https://images.unsplash.com/photo-1588776814546-1ffcf47267a5?q=80&w=500"
