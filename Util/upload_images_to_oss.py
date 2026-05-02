"""
Batch upload local images to Aliyun OSS.

Default behavior:
- Read OSS config from the repository root .env file.
- Walk D:/Junior/OmniSource/images.
- Upload each city folder into OSS under OmniSource/source/<city>/.

Run from repository root:
    python Util/upload_images_to_oss.py

Dry run:
    python Util/upload_images_to_oss.py --dry-run
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import hmac
import mimetypes
import os
from datetime import datetime, timezone
from email.utils import format_datetime
from pathlib import Path
from typing import Dict, Iterable, Tuple
from urllib import request
from urllib.parse import quote


IMAGE_EXTENSIONS = {
    ".jpg",
    ".jpeg",
    ".png",
    ".gif",
    ".webp",
    ".bmp",
    ".tif",
    ".tiff",
}


def load_env(path: Path) -> Dict[str, str]:
    values: Dict[str, str] = {}
    if not path.exists():
        return values

    for raw_line in path.read_text(encoding="utf-8-sig").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        values[key.strip()] = value.strip().strip('"').strip("'")
    return values


def normalize_prefix(value: str) -> str:
    value = (value or "").strip().replace("\\", "/")
    value = value.strip("/")
    return f"{value}/" if value else ""


def oss_object_key(prefix: str, local_root: Path, image_path: Path) -> str:
    relative = image_path.relative_to(local_root).as_posix()
    return normalize_prefix(prefix) + relative


def iter_images(root: Path) -> Iterable[Path]:
    for path in sorted(root.rglob("*")):
        if path.is_file() and path.suffix.lower() in IMAGE_EXTENSIONS:
            yield path


def content_type(path: Path) -> str:
    detected, _ = mimetypes.guess_type(path.name)
    return detected or "application/octet-stream"


def sign_oss_request(
    method: str,
    bucket: str,
    object_key: str,
    content_md5: str,
    content_type_value: str,
    date_value: str,
    access_key_secret: str,
) -> str:
    canonical_resource = f"/{bucket}/{object_key}"
    string_to_sign = "\n".join(
        [
            method,
            content_md5,
            content_type_value,
            date_value,
            canonical_resource,
        ]
    )
    digest = hmac.new(
        access_key_secret.encode("utf-8"),
        string_to_sign.encode("utf-8"),
        hashlib.sha1,
    ).digest()
    return base64.b64encode(digest).decode("ascii")


def upload_file(
    endpoint: str,
    bucket: str,
    access_key_id: str,
    access_key_secret: str,
    public_base_url: str,
    image_path: Path,
    object_key: str,
) -> str:
    data = image_path.read_bytes()
    md5 = base64.b64encode(hashlib.md5(data).digest()).decode("ascii")
    mime = content_type(image_path)
    date = format_datetime(datetime.now(timezone.utc), usegmt=True)
    signature = sign_oss_request(
        "PUT",
        bucket,
        object_key,
        md5,
        mime,
        date,
        access_key_secret,
    )

    encoded_key = "/".join(quote(part, safe="") for part in object_key.split("/"))
    url = f"https://{bucket}.{endpoint}/{encoded_key}"
    req = request.Request(url, data=data, method="PUT")
    req.add_header("Date", date)
    req.add_header("Content-Type", mime)
    req.add_header("Content-MD5", md5)
    req.add_header("Authorization", f"OSS {access_key_id}:{signature}")

    with request.urlopen(req, timeout=60) as response:
        if response.status not in (200, 201):
            raise RuntimeError(f"Unexpected OSS response status: {response.status}")

    return f"{public_base_url.rstrip('/')}/{encoded_key}"


def required(values: Dict[str, str], key: str) -> str:
    value = values.get(key) or os.environ.get(key)
    if not value:
        raise RuntimeError(f"Missing required config: {key}")
    return value


def main() -> int:
    repo_root = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description="Upload images directory to Aliyun OSS.")
    parser.add_argument("--images-dir", type=Path, default=repo_root / "images")
    parser.add_argument("--env-file", type=Path, default=repo_root / ".env")
    parser.add_argument("--oss-prefix", default="OmniSource/source/")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    env = load_env(args.env_file)
    endpoint = required(env, "ALIYUN_OSS_ENDPOINT")
    access_key_id = required(env, "ALIYUN_OSS_ACCESS_KEY_ID")
    access_key_secret = required(env, "ALIYUN_OSS_ACCESS_KEY_SECRET")
    bucket = required(env, "ALIYUN_OSS_BUCKET_NAME")
    public_base_url = env.get("ALIYUN_OSS_PUBLIC_BASE_URL") or f"https://{bucket}.{endpoint}"

    images_dir = args.images_dir.resolve()
    if not images_dir.exists():
        raise RuntimeError(f"Images directory does not exist: {images_dir}")

    images: Tuple[Path, ...] = tuple(iter_images(images_dir))
    if not images:
        print(f"No image files found under {images_dir}")
        return 0

    print(f"Images directory: {images_dir}")
    print(f"OSS target: oss://{bucket}/{normalize_prefix(args.oss_prefix)}")
    print(f"Image count: {len(images)}")

    success = 0
    for image_path in images:
        key = oss_object_key(args.oss_prefix, images_dir, image_path)
        if args.dry_run:
            print(f"[DRY] {image_path} -> {key}")
            continue

        try:
            url = upload_file(
                endpoint,
                bucket,
                access_key_id,
                access_key_secret,
                public_base_url,
                image_path,
                key,
            )
            success += 1
            print(f"[OK] {image_path.relative_to(images_dir)} -> {url}")
        except Exception as exc:
            print(f"[FAIL] {image_path.relative_to(images_dir)} -> {exc}")

    print(f"Done. Uploaded {success}/{len(images)} files.")
    return 0 if success == len(images) or args.dry_run else 1


if __name__ == "__main__":
    raise SystemExit(main())
