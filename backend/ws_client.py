#!/usr/bin/env python3
import argparse
import base64
import os
import socket
import ssl
import sys
import time
from hashlib import sha1
from urllib.parse import urlparse


def build_request(path, host, port, key, origin, cookie_token=None):
    host_header = host if (port == 80 or port == 443) else f"{host}:{port}"
    headers = [
        f"GET {path} HTTP/1.1",
        f"Host: {host_header}",
        "Upgrade: websocket",
        "Connection: keep-alive, Upgrade",
        f"Sec-WebSocket-Key: {key}",
        "Sec-WebSocket-Version: 13",
        f"Origin: {origin}",
    ]
    if cookie_token:
        headers.append(f"Cookie: authToken={cookie_token}")
    headers.extend(["", ""])
    return "\r\n".join(headers).encode("utf-8")


def send_frame(sock, opcode, payload=b""):
    length = len(payload)
    mask_key = os.urandom(4)
    masked_payload = bytes(b ^ mask_key[i % 4] for i, b in enumerate(payload))
    header = bytearray()
    header.append(0x80 | (opcode & 0x0F))
    if length < 126:
        header.append(0x80 | length)
    elif length < (1 << 16):
        header.append(0x80 | 126)
        header.extend(length.to_bytes(2, "big"))
    else:
        header.append(0x80 | 127)
        header.extend(length.to_bytes(8, "big"))
    header.extend(mask_key)
    sock.sendall(header + masked_payload)


def read_bytes(sock, buffer, count, timeout):
    start = time.time()
    while len(buffer) < count:
        if timeout and time.time() - start > timeout:
            return False
        chunk = sock.recv(4096)
        if not chunk:
            return False
        buffer.extend(chunk)
    return True


def read_frame(sock, buffer, timeout):
    if not read_bytes(sock, buffer, 2, timeout):
        return None
    b1, b2 = buffer[0], buffer[1]
    del buffer[:2]
    opcode = b1 & 0x0F
    length = b2 & 0x7F
    if length == 126:
        if not read_bytes(sock, buffer, 2, timeout):
            return None
        length = int.from_bytes(buffer[:2], "big")
        del buffer[:2]
    elif length == 127:
        if not read_bytes(sock, buffer, 8, timeout):
            return None
        length = int.from_bytes(buffer[:8], "big")
        del buffer[:8]
    if b2 & 0x80:
        if not read_bytes(sock, buffer, 4, timeout):
            return None
        mask_key = buffer[:4]
        del buffer[:4]
    else:
        mask_key = None
    if not read_bytes(sock, buffer, length, timeout):
        return None
    payload = bytes(buffer[:length])
    del buffer[:length]
    if mask_key:
        payload = bytes(b ^ mask_key[i % 4] for i, b in enumerate(payload))
    return opcode, payload


def connect(url, cookie_token=None):
    parsed = urlparse(url)
    if parsed.scheme not in ("ws", "wss"):
        raise ValueError("Unsupported scheme")
    host = parsed.hostname or "localhost"
    port = parsed.port or (443 if parsed.scheme == "wss" else 80)
    path = parsed.path or "/"
    if parsed.query:
        path += "?" + parsed.query
    sock = socket.create_connection((host, port), timeout=10)
    if parsed.scheme == "wss":
        ctx = ssl.create_default_context()
        sock = ctx.wrap_socket(sock, server_hostname=host)
    origin_scheme = "https" if parsed.scheme == "wss" else "http"
    origin = f"{origin_scheme}://{host}" + (f":{port}" if (parsed.port not in (None, 80, 443)) else "")
    key = base64.b64encode(os.urandom(16)).decode()
    request = build_request(path, host, port, key, origin, cookie_token)
    sock.sendall(request)
    response = b""
    while b"\r\n\r\n" not in response:
        chunk = sock.recv(4096)
        if not chunk:
            raise RuntimeError("Handshake failed")
        response += chunk
    header, remainder = response.split(b"\r\n\r\n", 1)
    if b" 101 " not in header.split(b"\r\n", 1)[0]:
        raise RuntimeError("Unexpected handshake response")
    accept_expected = base64.b64encode(sha1((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").encode()).digest())
    accept_value = None
    for line in header.split(b"\r\n"):
        lower = line.lower()
        if lower.startswith(b"sec-websocket-accept:"):
            accept_value = line.split(b":", 1)[1].strip()
            break
    if accept_value != accept_expected.strip():
        raise RuntimeError("Invalid Sec-WebSocket-Accept header")
    return sock, bytearray(remainder)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--duration", type=float, default=30.0)
    parser.add_argument("--idle-timeout", dest="idle_timeout", type=float, default=2.0)
    parser.add_argument("--cookie-token", dest="cookie_token")
    args = parser.parse_args()
    sock, buffer = connect(args.url, args.cookie_token)
    end_time = time.time() + args.duration
    with open(args.output, "w", encoding="utf-8") as handle:
        try:
            while time.time() < end_time:
                frame = read_frame(sock, buffer, args.idle_timeout)
                if frame is None:
                    continue
                opcode, payload = frame
                if opcode == 1:
                    try:
                        text = payload.decode("utf-8")
                    except UnicodeDecodeError:
                        text = payload.decode("utf-8", errors="ignore")
                    handle.write(text + "\n")
                    handle.flush()
                elif opcode == 9:
                    send_frame(sock, 0xA, payload)
                elif opcode == 8:
                    break
        finally:
            try:
                send_frame(sock, 0x8)
            except Exception:
                pass
            sock.close()


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        sys.stderr.write(f"WebSocket client error: {exc}\n")
        sys.exit(1)

