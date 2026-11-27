"""Drive the demo app through MCP tool calls, as an agent would."""

import allure

from mcp_client import vibium_mcp

pytestmark = [allure.epic("Vibium MCP server"), allure.feature("Browsing")]


async def sign_in(mcp, base_url, password="tide-pass"):
    await mcp.ok("browser_navigate", url=base_url + "index.html")
    await mcp.ok("browser_fill", selector="#username", text="demo")
    await mcp.ok("browser_fill", selector="#password", text=password)
    await mcp.ok("browser_click", selector="[data-testid=sign-in]")


async def test_sign_in_flow(base_url):
    async with vibium_mcp() as mcp:
        await sign_in(mcp, base_url)
        await mcp.ok("browser_wait_for_url", pattern="/board.html", timeout=10000)
        greeting = await mcp.ok("browser_get_text", selector="[data-testid=greeting]")
        assert greeting.text == "Signed in as demo"


async def test_wrong_password_message(base_url):
    async with vibium_mcp() as mcp:
        await sign_in(mcp, base_url, password="wrong")
        await mcp.ok("browser_wait_for_text", text="Wrong username or password.", timeout=10000)
        url = await mcp.ok("browser_get_url")
        assert url.text.endswith("/index.html")


async def test_map_refs_can_be_acted_on(base_url):
    async with vibium_mcp() as mcp:
        await mcp.ok("browser_navigate", url=base_url + "index.html")
        mapped = await mcp.ok("browser_map")
        lines = mapped.text.splitlines()
        sign_in_ref = next(line.split()[0] for line in lines if '"Sign in"' in line)
        assert sign_in_ref.startswith("@e")
        await mcp.ok("browser_click", selector=sign_in_ref)
        await mcp.ok("browser_wait_for_text", text="Enter a username and a password.", timeout=5000)


async def test_add_and_complete_task(base_url):
    async with vibium_mcp() as mcp:
        await sign_in(mcp, base_url)
        await mcp.ok("browser_wait_for_url", pattern="/board.html", timeout=10000)
        await mcp.ok("browser_fill", selector="[data-testid=new-task]", text="Check MCP results")
        await mcp.ok("browser_press", key="Enter", selector="[data-testid=new-task]")
        count = await mcp.ok("browser_count", selector="[data-testid=task]")
        assert count.text == "1"
        await mcp.ok("browser_set", selector="[data-testid=toggle]")
        state = await mcp.ok("browser_is_set", selector="[data-testid=toggle]")
        assert state.text.lower() == "true"
        remaining = await mcp.ok("browser_get_text", selector="[data-testid=remaining]")
        assert remaining.text == "0 tasks left"


async def test_screenshot_returns_png(base_url):
    async with vibium_mcp() as mcp:
        await mcp.ok("browser_navigate", url=base_url + "index.html")
        shot = await mcp.ok("browser_screenshot")
        assert shot.images, "no image content block"
        assert shot.images[0][:8] == b"\x89PNG\r\n\x1a\n"


async def test_evaluate_reads_app_state(base_url):
    async with vibium_mcp() as mcp:
        await sign_in(mcp, base_url)
        await mcp.ok("browser_wait_for_url", pattern="/board.html", timeout=10000)
        stored = await mcp.ok("browser_evaluate", expression="localStorage.getItem('tp-user')")
        assert stored.text.strip('"') == "demo"
