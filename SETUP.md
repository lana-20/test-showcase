# Connecting the other CIs and cloud services

Everything here is free. Nothing in this guide needs a credit card, except where a step says so.

| Suite or leg | Target | Until connected | You create | GitHub secret(s) | GitHub variable(s) |
|---|---|---|---|---|---|
| Playwright | GitHub Actions | (already live) | — | — | — |
| Appium Java | Jenkins (Jenkinsfile Runner in GitHub Actions) | (already live) | — | — | — |
| Vibium Python | GitLab CI | runs on GitHub Actions | GitLab account and public project | `GITLAB_PUSH_TOKEN` (optional `GITLAB_READ_TOKEN`) | `GITLAB_PROJECT`, then `VIBIUM_PYTHON_CI=gitlab-ci` |
| Vibium MCP | Azure Pipelines | runs on GitHub Actions | Azure DevOps organization and project | `AZURE_DEVOPS_TOKEN` | `AZURE_DEVOPS_ORG`, `AZURE_DEVOPS_PROJECT`, then `VIBIUM_MCP_CI=azure-pipelines` |
| Mobium | CircleCI | runs on GitHub Actions | CircleCI account, project followed | `CIRCLECI_TOKEN` | then `MOBIUM_CI=circleci` |
| Playwright and Vibium on Kernel | Kernel cloud browsers | "ready, pending account" | Kernel account and API key | `KERNEL_API_KEY` | — |
| Real-device Robo | Firebase Test Lab | "ready, pending account" | Firebase project (Spark plan) | `GCP_SA_KEY` (only if you skip keyless auth) | `GCP_PROJECT`, `GCP_WIF_PROVIDER`, `GCP_SERVICE_ACCOUNT` |

**Secrets: you set them yourself, in your own terminal.** `gh secret set NAME -R lana-20/test-showcase` asks for the
value without echoing it. Never paste a token into a chat, an issue, a file or a commit. Variables are not secret:
`gh variable set NAME -R lana-20/test-showcase -b VALUE`.

When the last variable for a suite is set (for example `VIBIUM_PYTHON_CI=gitlab-ci`), GitHub Actions stops running
that suite itself and downloads its results from the other CI instead. The card then says "Ran on: GitLab CI". You
can set it yourself or leave it to me after I've checked the first run there.

---

## 1. GitLab CI → Vibium (Python client)

GitHub stays the source of truth. GitLab's pull mirroring and "CI/CD for external repositories" are paid tiers
(Premium and Ultimate: [pull mirroring](https://docs.gitlab.com/user/project/repository/mirror/pull/),
[external repos](https://docs.gitlab.com/ci/ci_cd_for_external_repos/)). So the GitHub workflow pushes `main` to a
public GitLab project, and that push triggers `.gitlab-ci.yml`.

1. Go to https://gitlab.com/users/sign_up and pick **Continue with GitHub** (or use an email). Choose the Free tier if asked.
2. **New project → Create blank project**
   - Project name: `test-showcase`
   - Visibility: **Public**
   - **Uncheck** "Initialize repository with a README" (the first push from GitHub fills it).
   - Click **Create project**.
3. Create a token. Project access tokens need Premium on GitLab.com
   ([docs](https://docs.gitlab.com/user/project/settings/project_access_tokens/)), so use a personal access token:
   - Avatar → **Edit profile → Access tokens → Add new token**
   - Name: `github-mirror-test-showcase`
   - Expiration: pick a date. Up to one year is fine, and a calendar reminder helps.
   - Scopes: **write_repository** (to push) and **read_api** (to read job artifacts)
   - **Create token**, and copy it once.
4. In your terminal:
   ```sh
   gh secret set GITLAB_PUSH_TOKEN -R lana-20/test-showcase          # paste the token when asked
   gh variable set GITLAB_PROJECT -R lana-20/test-showcase -b "<your-gitlab-username>/test-showcase"
   ```
   Optional, least privilege: create a second token with only `read_api` and save it as `GITLAB_READ_TOKEN`.
   Without it, the report job reads with the push token.
5. Push anything to `main`, or re-run the Showcase workflow. The **Mirror main to GitLab** job pushes the code, and
   the GitLab pipeline starts.
   - GitLab may ask you to verify your identity before its shared runners run a job. Use the phone option if it's offered.
6. Nightly run: in GitLab, **Build → Pipeline schedules → New schedule**
   - Description: `Nightly`
   - Interval pattern: custom `15 7 * * *`
   - Cron timezone: UTC
   - Target branch: `main`
   - **Create pipeline schedule**.

**Compute minutes:** the Free tier has 400 compute minutes a month. One run pulls the image and runs nine tests,
about 3–4 minutes. Nightly plus about 20 pushes a month comes to roughly 200 minutes, half the allowance. Check
**Settings → Usage quotas** after the first week.

---

## 2. Azure Pipelines → Vibium MCP server

1. Go to https://dev.azure.com and sign in with a Microsoft account or **Sign in with GitHub**. No card is needed.
2. Create an organization if asked (any name, for example `lana-20`). Note the name: it is `AZURE_DEVOPS_ORG`.
3. To make the project public, first allow public projects: **Organization settings → Policies → Allow public
   projects → On**. Then **New project**: name `test-showcase`, visibility **Public**, **Create**.
4. Hosted build minutes. New organizations often start with no free Microsoft-hosted parallel job. If your first run
   says "No hosted parallelism has been purchased or granted", fill in the free grant form at
   https://aka.ms/azpipelines-parallelism-request (choose the public-project option). Microsoft says it can take a
   few business days.
5. **Pipelines → New pipeline → GitHub (YAML)**
   - When asked, install the **Azure Pipelines** GitHub App and give it access to **only** `lana-20/test-showcase`.
   - Select the repo → **Existing Azure Pipelines YAML file** → branch `main`, path `/azure-pipelines.yml` → **Run**.
   - The YAML has its own push trigger and nightly schedule (07:30 UTC). Nothing else to set.
6. Read-only token for the GitHub report job:
   - User settings (top right) → **Personal access tokens → New Token**
   - Name: `github-pages-read`
   - Organization: yours
   - Expiration: your choice (up to one year)
   - Scopes: **Custom defined → Build: Read** (nothing else)
   - **Create**, and copy it once.
7. In your terminal:
   ```sh
   gh secret set AZURE_DEVOPS_TOKEN -R lana-20/test-showcase
   gh variable set AZURE_DEVOPS_ORG -R lana-20/test-showcase -b "<organization>"
   gh variable set AZURE_DEVOPS_PROJECT -R lana-20/test-showcase -b "test-showcase"
   ```

---

## 3. CircleCI → Mobium

The Mobium suite needs an Android emulator, which needs KVM. CircleCI's Android machine images provide it.
GitLab's hosted runners don't reliably offer it, which is why Mobium is on CircleCI and Vibium Python is on GitLab.

1. Go to https://circleci.com/signup and choose **Sign up with GitHub**. Authorize the CircleCI app for
   `lana-20/test-showcase` only, if it asks which repos.
2. **Projects → test-showcase → Set Up Project**. Choose the existing config: branch `main`, `.circleci/config.yml`.
   CircleCI starts the first pipeline.
3. Nightly run: **Project Settings → Triggers** (called "Schedule triggers" in some layouts) → **Add Trigger**
   - Name: `nightly`
   - Branch: `main`
   - Every day at 07:00 UTC
   - **Save**.
4. Token for the GitHub report job. Try a read-only project token first:
   - **Project Settings → API Permissions → Add API Token**
   - Scope: **Read Only**
   - Label: `github-pages-read`
   - Then run `gh secret set CIRCLECI_TOKEN -R lana-20/test-showcase`.
   - If downloading artifacts fails with that token, the fallback is a personal token (**User Settings → Personal
     API Tokens**). That token can access your whole CircleCI account, so treat it carefully.
5. **Free plan usage:** check **Plan → Usage** after the first run. The job boots an emulator on a `large` Android
   machine, about 10 minutes per run. I could not confirm the exact credit rate for that resource class on the Free
   plan. If nightly runs use too much, the schedule can drop to a few times a week.

---

## 4. Kernel → Playwright and Vibium in a cloud browser

Kernel's pricing FAQ says you can sign up "without a credit card"
([pricing](https://www.kernel.sh/pricing)). The free plan has 5 concurrent browsers and $5 of usage a month. Its
[acceptable-use policy](https://www.kernel.sh/docs/acceptable-use) says Kernel *may* ask for verification
before granting free credit.

1. Go to https://www.kernel.sh and choose **Sign up** (GitHub login works).
2. Dashboard → **API Keys → Create API key**. Name it `github-test-showcase` and copy it once.
3. Run `gh secret set KERNEL_API_KEY -R lana-20/test-showcase`.

What runs: nightly (and on a manual run), one headless browser for the Playwright suite over CDP, then one for the
Vibium suite over WebDriver BiDi. Each uses one worker, and each session is deleted in a shell `trap` when its
script exits. A 120-second idle timeout on Kernel's side and a 10-minute hard timeout in CI back that up. The cloud
browser can't reach a CI runner, so it tests the public copy of the demo app at
https://lana-20.github.io/test-showcase/demo/. Expected use is a few browser-minutes a day, well inside the free
credit. Check Kernel's usage page after the first week.

Vibium can't use Kernel's CDP URL. Vibium speaks W3C protocols only, so it connects with Kernel's
`webdriver_ws_url` (WebDriver BiDi) through `VIBIUM_CONNECT_URL`. Both
[Vibium's Kernel guide](https://github.com/VibiumDev/vibium/blob/main/docs/how-to-guides/cloud-browsers/kernel.md)
and [Kernel's docs](https://kernel.sh/docs/integrations/vibium) document this.

---

## 5. Firebase Test Lab → Robo crawl on a real Android phone

> **Retirement, confirmed by Google:** this is not just a blog claim. The Firebase release notes of September 30,
> 2026 say: "Firebase Test Lab is deprecated and will be fully shut down as of September 30, 2027"
> ([release notes](https://firebase.google.com/support/releases), [migration guide](https://firebase.google.com/docs/test-lab/migrate)).
> The replacement, Developer Device Platform on Google Cloud, is in preview and **requires billing**
> ([FAQ](https://firebase.google.com/docs/test-lab/migrate-faq)). So this leg is free only until Test Lab shuts down.

The Spark plan (no card) allows 10 virtual-device and 5 physical-device runs a day
([quotas](https://firebase.google.com/docs/test-lab/usage-quotas-pricing)). This leg runs one of each a night.

1. Go to https://console.firebase.google.com → **Create a project** → name it `test-showcase-lab`.
   - Turn **off** Google Analytics.
   - **Create project**. New projects are on Spark.
   - Use a separate project. Without billing, Test Lab writes results only to its default bucket, and that needs a
     broad role (step 3). A dedicated project keeps that role contained.
2. Turn on the two APIs Test Lab uses, in the Google Cloud console for the same project:
   - https://console.cloud.google.com/apis/library/testing.googleapis.com → **Enable**
   - https://console.cloud.google.com/apis/library/toolresults.googleapis.com → **Enable**
3. Create a service account: **IAM & Admin → Service Accounts → Create**
   - Name: `testlab-ci`
   - Role: **Editor**
   - Google's IAM reference says the default results bucket needs Editor. The narrower Test Lab roles only work with
     your own bucket, and that needs billing
     ([IAM reference](https://firebase.google.com/docs/test-lab/android/iam-permissions-reference)).
4. Let GitHub Actions use it. Choose one option.

   **A. Keyless (preferred): Workload Identity Federation.** Click the Cloud Shell icon at the top right of the
   console, then run:
   ```sh
   PROJECT_ID=test-showcase-lab        # your project ID, shown on the console home page
   gcloud iam workload-identity-pools create github --project="$PROJECT_ID" --location=global --display-name="GitHub"
   gcloud iam workload-identity-pools providers create-oidc test-showcase --project="$PROJECT_ID" \
     --location=global --workload-identity-pool=github \
     --issuer-uri="https://token.actions.githubusercontent.com" \
     --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
     --attribute-condition="assertion.repository=='lana-20/test-showcase'"
   POOL=$(gcloud iam workload-identity-pools describe github --project="$PROJECT_ID" --location=global --format='value(name)')
   gcloud iam service-accounts add-iam-policy-binding "testlab-ci@$PROJECT_ID.iam.gserviceaccount.com" \
     --project="$PROJECT_ID" --role=roles/iam.workloadIdentityUser \
     --member="principalSet://iam.googleapis.com/$POOL/attribute.repository/lana-20/test-showcase"
   gcloud iam workload-identity-pools providers describe test-showcase --project="$PROJECT_ID" \
     --location=global --workload-identity-pool=github --format='value(name)'     # this is GCP_WIF_PROVIDER
   ```
   Then, in your own terminal:
   ```sh
   gh variable set GCP_PROJECT -R lana-20/test-showcase -b "test-showcase-lab"
   gh variable set GCP_WIF_PROVIDER -R lana-20/test-showcase -b "projects/…/locations/global/workloadIdentityPools/github/providers/test-showcase"
   gh variable set GCP_SERVICE_ACCOUNT -R lana-20/test-showcase -b "testlab-ci@test-showcase-lab.iam.gserviceaccount.com"
   ```
   I couldn't find a Google page that confirms Workload Identity Federation works on a project with no billing
   account. If a command above is refused, use option B.

   **B. Fallback: a service account key.** **IAM & Admin → Service Accounts → testlab-ci → Keys → Add key → JSON**.
   Then:
   ```sh
   gh secret set GCP_SA_KEY -R lana-20/test-showcase < ~/Downloads/<the-key-file>.json
   rm ~/Downloads/<the-key-file>.json
   gh variable set GCP_PROJECT -R lana-20/test-showcase -b "test-showcase-lab"
   ```
5. On the next nightly or manual run, the job picks a Google phone of each kind from the live device catalog (the
   newest Android version it supports, skipping reduced-quota models). It runs a 5-minute Robo crawl of ApiDemos and
   attaches the screenshots and the video to an Allure report. The Appium card then shows "Ran on a real <model> via
   Firebase Test Lab".

---

## 6. When you're done, tell me

Send a short note per service, with **no tokens**:

- "GitLab done, project `<username>/test-showcase`, schedule created."
- "Azure done, org `<org>`, project `test-showcase`, the parallelism grant is approved (or pending)."
- "CircleCI done, read-only project token set (or: had to use a personal token)."
- "Kernel key set."
- "Firebase done, option A or B."

I will run the workflow and check that each suite really ran on its own CI, then switch the variables and labels,
and confirm the cards on the live page.
