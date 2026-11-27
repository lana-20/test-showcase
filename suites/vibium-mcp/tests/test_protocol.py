"""The MCP contract itself: handshake, tool catalog and schemas."""

import allure

from mcp_client import vibium_mcp

pytestmark = [allure.epic("Vibium MCP server"), allure.feature("Protocol")]

CORE_TOOLS = {
    "browser_navigate", "browser_click", "browser_fill", "browser_press",
    "browser_get_text", "browser_get_title", "browser_get_url", "browser_screenshot",
    "browser_map", "browser_wait_for_text", "browser_wait_for_url", "browser_count",
    "browser_evaluate", "browser_stop",
}


async def test_handshake_identifies_vibium():
    async with vibium_mcp() as mcp:
        assert mcp.server_info.name == "vibium"
        assert mcp.server_info.version


async def test_tool_catalog_has_core_tools_with_object_schemas():
    async with vibium_mcp() as mcp:
        tools = await mcp.tools()
        missing = CORE_TOOLS - tools.keys()
        assert not missing, f"missing tools: {sorted(missing)}"
        for name in CORE_TOOLS:
            schema = tools[name].input_schema
            assert schema.get("type") == "object", name
            assert tools[name].description, f"{name} has no description"
        assert "url" in tools["browser_navigate"].input_schema.get("required", [])


async def test_tool_error_is_reported_and_session_survives(base_url):
    async with vibium_mcp() as mcp:
        await mcp.ok("browser_navigate", url=base_url + "index.html")
        missing = await mcp.call("browser_click", selector="#does-not-exist", timeout=1000)
        assert missing.is_error
        assert "not found" in missing.text.lower()
        # A tool error is a result, not a broken connection: keep going.
        title = await mcp.ok("browser_get_title")
        assert title.text == "Tidepool · Sign in"


async def test_unknown_tool_is_an_error_result():
    async with vibium_mcp() as mcp:
        result = await mcp.call("browser_teleport")
        assert result.is_error
        assert "unknown tool" in result.text.lower()
