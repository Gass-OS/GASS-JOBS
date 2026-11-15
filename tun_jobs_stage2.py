from pathlib import Path
import sys

path = Path("app/src/main/java/com/gass/jobs/MainActivity.java")
s = path.read_text(encoding="utf-8")
original = s

def replace_once(old, new, label):
    global s
    count = s.count(old)
    if count != 1:
        sys.exit(f"STOP: {label}: expected 1 match, found {count}. No Java changes written.")
    s = s.replace(old, new, 1)

# Add ANETI International to the existing search results.
start = s.index("    private void renderJobs()")
end = s.index("    private void showOffers()", start)
part = s[start:end]

anchor = "        for (Job job : demoJobs) {"
if part.count(anchor) != 1:
    sys.exit("STOP: renderJobs insertion point mismatch.")

aneti_loop = '''        for (Job job : anetiJobs) {
            String searchable = (job.title + " " + job.company + " " + job.location + " "
                    + job.governorate + " " + job.sector + " " + job.contract + " "
                    + job.description).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !searchable.contains(query)) continue;
            if (!"Tous".equals(governorate) && !job.governorate.equalsIgnoreCase(governorate)) continue;
            if (!"Tous".equals(sector) && !job.sector.equalsIgnoreCase(sector)) continue;
            if (!"Tous".equals(contract) && !job.contract.equalsIgnoreCase(contract)) continue;
            visibleJobs.add(job);
        }
'''
part = part.replace(anchor, aneti_loop + anchor, 1)
s = s[:start] + part + s[end:]

replace_once(
'''        resultsContainer.addView(text(publishedJobs.size()+" offre(s) publiée(s) par les employeurs • annonces de démonstration clairement séparées.", 12, muted, false));''',
'''        resultsContainer.addView(text(publishedJobs.size()+" offre(s) employeur publiée(s) • "+anetiJobs.size()+" fiche(s) ANETI International enregistrée(s) • annonces de démonstration séparées.", 12, muted, false));
        space(resultsContainer, 4);
        resultsContainer.addView(text("Les fiches ANETI International sont issues d'un relevé consulté le 10/11/2026, pas d'un flux en temps réel. Vérifiez leur disponibilité sur la fiche officielle avant de postuler. ANETI national, Idaraty et les concours publics restent accessibles séparément dans les sources officielles.", 12, muted, false));''',
"search freshness")

replace_once(
'''            item.addView(text(job.company + " • " + job.location, 14, muted, false));
            space(item, 10);''',
'''            item.addView(text(job.company + " • " + job.location, 14, muted, false));
            if (job.isOfficial()) {
                space(item, 4);
                item.addView(text("SOURCE : ANETI INTERNATIONAL • Publié : "
                        + (job.publishedDate.isEmpty() ? "date non indiquée" : job.publishedDate),
                        12, gold, true));
                item.addView(text("Relevé du 10/11/2026 — disponibilité à confirmer sur la fiche originale.",
                        11, muted, false));
            }
            space(item, 10);''',
"search card source")

replace_once(
'''            item.addView(button("Voir l'offre", true,
                    () -> showJob(job.title, job.company, job.location, job.description)),''',
'''            item.addView(button(job.isOfficial() ? "Vérifier sur ANETI" : "Voir l'offre", true,
                    () -> {
                        if (job.isOfficial()) showOfficialJob(job);
                        else showJob(job.title, job.company, job.location, job.description);
                    }),''',
"search card action")

replace_once(
'''        String badge = job.isPublishedEmployerOffer() ? "✓ OFFRE EMPLOYEUR PUBLIÉE"
                : (job.isOfficial() ? "✓ ANETI • OFFRE OFFICIELLE" : "● Annonce de démonstration");''',
'''        String badge = job.isPublishedEmployerOffer() ? "✓ OFFRE EMPLOYEUR PUBLIÉE"
                : (job.isOfficial() ? "ANETI • FICHE SOURCE À VÉRIFIER" : "● Annonce de démonstration");''',
"swipe badge")

replace_once(
'''        base(job.title, "ANETI • " + job.location, TAB_OFFERS, true);
        content.addView(chip("✓ OFFRE PUBLIÉE PAR ANETI", true));''',
'''        base(job.title, "ANETI International • " + job.location, TAB_OFFERS, true);
        content.addView(chip("SOURCE OFFICIELLE • DISPONIBILITÉ À CONFIRMER", true));''',
"ANETI detail header")

replace_once(
'''        content.addView(text("Source : ANETI International. La date limite n'était pas indiquée dans la fiche consultée. Vérifiez que l'offre est toujours ouverte avant de préparer votre candidature.", 13, muted, false));''',
'''        content.addView(text("Source : ANETI International. Relevé consulté le 10/11/2026 ; cette date ne correspond pas à une actualisation en direct. La date limite n'était pas indiquée dans la fiche consultée. Vérifiez que l'offre est toujours ouverte et que ses conditions n'ont pas changé avant de postuler.", 13, muted, false));''',
"ANETI detail disclosure")

replace_once(
'''        infoCard("Idaraty.tn", "Portail national des services administratifs. Les avis de recrutement doivent être confirmés sur la page de l'organisme émetteur.", "Ouvrir Idaraty", () -> openExternal("https://www.idaraty.tn/"));''',
'''        infoCard("Idaraty — recrutement", "Page officielle de recrutement propre à Idaraty ; ce n'est pas une base générale de toutes les offres d'emploi.", "Ouvrir Idaraty recrute", () -> openExternal("https://www.idaraty.tn/public/index.php/fr/recrute"));''',
"Idaraty monitoring link")

replace_once(
'''        infoCard("ANETI — Agence nationale pour l'emploi", "Portail national de l'emploi et des services aux demandeurs d'emploi et aux entreprises. Les offres doivent être vérifiées sur le site source.", "Ouvrir emploi.nat.tn", () -> openExternal("https://www.emploi.nat.tn/"));''',
'''        infoCard("ANETI — offres nationales", "Portail national de l'emploi. Consultez directement les offres nationales ; elles ne sont pas encore synchronisées automatiquement dans l'application.", "Ouvrir les offres ANETI Tunisie", () -> openExternal(ANETI_TUNISIA_OFFERS));
        infoCard("ANETI International", "Offres d'emploi à l'étranger. Les fiches reprises dans le carrousel sont un relevé daté et doivent être revérifiées sur le site source.", "Ouvrir les offres internationales", () -> openExternal(ANETI_INTERNATIONAL_OFFERS));
        infoCard("Idaraty — recrutement", "Page de recrutement propre à Idaraty ; elle ne constitue pas un agrégateur général d'offres.", "Ouvrir Idaraty recrute", () -> openExternal("https://www.idaraty.tn/public/index.php/fr/recrute"));''',
"official sources")

if s == original:
    sys.exit("STOP: no modifications produced.")

path.write_text(s, encoding="utf-8")
print("PATCH APPLIED: MainActivity.java")
print("ANETI International added to search; employer and demo listings preserved.")
print("ThemeColors.java untouched.")
