# Claims manager report (Power BI)

A one-page report for a claims manager, built in Power BI Desktop on the read-only reporting views from
migration `V8__create_reporting_views.sql`. Every number in it comes from synthetic data.

## Connect

1. Create the login with `create-report-login.sql` (it can read the six `rpt_*` views and nothing else).
2. In Power BI Desktop choose Get data, PostgreSQL database, enter the server and `notsureance`, and sign in
   as `powerbi_reader`. On Azure the server is `<prefix>-pg-<suffix>.postgres.database.azure.com`.
3. Load all six `rpt_*` views. Import mode is enough at this data size.
4. Apply `notsureance-theme.json` (View, Themes, Browse for themes) and paste the measures from `measures.dax`.

## Page layout

| Visual | Fields |
| --- | --- |
| KPI cards | Claims Filed, Fast Track Share, Approved Payouts, Loss Ratio, Agent Override Rate |
| Clustered bar: loss ratio by equipment type, split by state | `rpt_loss_ratio_by_segment` |
| Funnel: filed, scored, fast tracked, investigated, confirmed fraud, paid | `rpt_fraud_funnel` sorted by `stage_order` |
| Bar: median days from filing to decision per adjuster | `rpt_adjuster_cycle_time` |
| Column: fraud rate by k-means segment (Steady, Watch, Elevated) | `rpt_fraud_by_segment` |
| Stacked bar: agent proposals approved versus overridden per action | `rpt_agent_proposals` |
| Text box | "Synthetic data. Not real farms, people or claims." |

The override visual is the one to lead with: it shows how often adjusters disagree with the agent.

## Share

Publishing to the Power BI service needs a work account, so export the page to PDF (File, Export, PDF) and
save it with a screenshot under `docs/assets/`, and commit the `.pbix` next to this file.
