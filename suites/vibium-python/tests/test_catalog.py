import allure
import pytest

from pages.catalog import CatalogPage
from pages.login import DEMO_USER, LoginPage

pytestmark = [allure.epic("Tidepool web app"), allure.feature("Catalog")]


@pytest.fixture
def catalog(vibe, base_url):
    LoginPage(vibe, base_url).open("index.html?next=catalog.html").sign_in(*DEMO_USER)
    # The sign-in form sends us on to the catalog; wait for that navigation
    # instead of starting a second one that would race it.
    return CatalogPage(vibe, base_url).open(navigate=False)


def test_search_narrows_results(catalog):
    catalog.wait_count("result", 6)
    catalog.search("galaxy")
    catalog.wait_text("status", "1 device")
    catalog.search("nokia")
    catalog.wait_text("status", "No devices match.")


def test_platform_filter(catalog):
    catalog.choose_platform("android")
    catalog.wait_text("status", "3 devices")


def test_reserve_devices(catalog):
    catalog.add_to_cart("Pixel 8")
    catalog.add_to_cart("iPad Air")
    catalog.wait_text("cart-count", "2")
    with allure.step("Reserve the devices"):
        catalog.page.find(role="button", text="Reserve devices").click()
    catalog.wait_text("confirmation", "Reserved 2 devices.")
    catalog.wait_text("cart-count", "0")
