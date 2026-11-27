"""A thin, test-friendly MCP client for Vibium's MCP server.

The tests talk to `vibium mcp` exactly the way an agent host does: they
spawn it over stdio, run the MCP handshake, list tools and call them. Every
tool call is an Allure step with the request and the response attached."""

from __future__ import annotations

import base64
import contextlib
import json
import os
from dataclasses import dataclass
from typing import Any, AsyncIterator

import allure
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client
from vibium.binary import find_vibium_bin


@dataclass
class ToolResult:
    is_error: bool
    texts: list[str]
    images: list[bytes]

    @property
    def text(self) -> str:
        return "\n".join(self.texts)


class VibiumMCP:
    def __init__(self, session: ClientSession, server_info: Any):
        self.session = session
        self.server_info = server_info

    async def tools(self) -> dict[str, Any]:
        with allure.step("tools/list"):
            listed = await self.session.list_tools()
            names = sorted(t.name for t in listed.tools)
            allure.attach("\n".join(names), name="tools", attachment_type=allure.attachment_type.TEXT)
            return {t.name: t for t in listed.tools}

    async def call(self, name: str, **arguments: Any) -> ToolResult:
        with allure.step(f"tools/call {name}"):
            allure.attach(json.dumps(arguments, indent=2), name="arguments",
                          attachment_type=allure.attachment_type.JSON)
            raw = await self.session.call_tool(name, arguments)
            texts, images = [], []
            for block in raw.content:
                if block.type == "text":
                    texts.append(block.text)
                elif block.type == "image":
                    images.append(base64.b64decode(block.data))
            result = ToolResult(bool(raw.is_error), texts, images)
            allure.attach(("ERROR: " if result.is_error else "") + result.text or "(no text)",
                          name="result", attachment_type=allure.attachment_type.TEXT)
            for i, png in enumerate(images):
                allure.attach(png, name=f"image {i + 1}", attachment_type=allure.attachment_type.PNG)
            return result

    async def ok(self, name: str, **arguments: Any) -> ToolResult:
        """Call a tool and fail the test, with the server's message, if it errors."""
        result = await self.call(name, **arguments)
        assert not result.is_error, f"{name} failed: {result.text}"
        return result


@contextlib.asynccontextmanager
async def vibium_mcp() -> AsyncIterator[VibiumMCP]:
    args = ["mcp", "--screenshot-dir", ""]
    if os.environ.get("HEADED") != "1":
        args.append("--headless")
    params = StdioServerParameters(command=find_vibium_bin(), args=args)
    allure.dynamic.parameter("server", "vibium " + " ".join(args))
    async with stdio_client(params) as (read, write):
        async with ClientSession(read, write) as session:
            init = await session.initialize()
            client = VibiumMCP(session, init.server_info)
            try:
                yield client
            except BaseException:
                # Screenshot on failure, taken through the same MCP session.
                with contextlib.suppress(Exception):
                    shot = await session.call_tool("browser_screenshot", {})
                    for block in shot.content:
                        if block.type == "image":
                            allure.attach(base64.b64decode(block.data), name="screenshot on failure",
                                          attachment_type=allure.attachment_type.PNG)
                raise
            finally:
                with contextlib.suppress(Exception):
                    await session.call_tool("browser_stop", {})
