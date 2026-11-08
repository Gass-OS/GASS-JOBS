package com.gass.jobs;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final int ink = Color.rgb(22, 35, 52);
    private final int muted = Color.rgb(104, 119, 135);
    private final int green = Color.rgb(0, 145, 105);
    private final int canvas = Color.rgb(246, 248, 250);
    private LinearLayout root;
    private LinearLayout content;
    private boolean employerMode = false;
    private EditText searchInput;
    private Spinner governorateFilter;
    private Spinner sectorFilter;
    private Spinner contractFilter;

    private static class Job {
        final String title, company, location, sector, contract, description;
        Job(String title, String company, String location, String sector, String contract, String description) {
            this.title = title;
            this.company = company;
            this.location = location;
            this.sector = sector;
            this.contract = contract;
            this.description = description;
        }
    }

    // Fictional examples used only to demonstrate the search UI.
    private final List<Job> demoJobs = new ArrayList<Job>() {{
        add(new Job("Agent commercial", "Entreprise de démonstration", "Kasserine", "Commerce", "Temps plein",
                "Relation client, prospection et suivi commercial. Expérience souhaitée."));
        add(new Job("Technicien informatique", "Entreprise de démonstration", "Sbeitla", "Informatique", "CDI",
                "Assistance informatique, maintenance et support aux utilisateurs."));
        add(new Job("Ouvrier agricole", "Exploitation de démonstration", "Fériana", "Agriculture", "Saisonnier",
                "Travaux agricoles. Débutants acceptés, formation sur place."));
        add(new Job("Électricien bâtiment", "Société fictive", "Kairouan", "Bâtiment", "CDD",
                "Installation électrique et maintenance sur chantiers. Exemple fictif."));
        add(new Job("Aide-soignant", "Centre de démonstration", "Sfax", "Santé", "Temps plein",
                "Accompagnement des patients et aide aux soins. Exemple fictif."));
        add(new Job("Chauffeur-livreur", "Entreprise fictive", "Tunis", "Transport", "CDI",
                "Livraison locale et suivi des commandes. Exemple fictif."));
    }};

    private static final String MINISTRY_DIRECTORY =
            "https://www.emploi.gov.tn/fr/93/directions-regionales-de-formation-professionnelle-et-de-lemploi";
    private static final String OPEN_DATA_DIRECTORY =
            "https://catalog.data.gov.tn/fr/dataset/reseau-de-bureaux-de-l-emploi-et-du-travail-independant-en-tunisie";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        showHome();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private void space(LinearLayout parent, int height) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, height));
    }

    private Button button(String label, boolean primary, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(primary ? Color.WHITE : ink);
        b.setBackground(shape(primary ? green : Color.rgb(232, 238, 242), 24));
        b.setPadding(12, 5, 12, 5);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Impossible d'ouvrir le lien.", Toast.LENGTH_SHORT).show();
        }
    }

    private void base(String title, String subtitle) {
        root = column();
        root.setBackgroundColor(canvas);
        setContentView(root);

        LinearLayout header = column();
        header.setPadding(22, 20, 22, 18);
        header.setBackgroundColor(Color.WHITE);
        header.addView(text("GASS-JOBS", 13, green, true));
        space(header, 8);
        header.addView(text(title, 25, ink, true));
        header.addView(text(subtitle, 14, muted, false));
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        content = column();
        content.setPadding(18, 18, 18, 24);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = row();
        nav.setBackgroundColor(Color.WHITE);
        nav.setPadding(5, 8, 5, 8);
        nav.addView(button("Emplois", false, this::showHome), new LinearLayout.LayoutParams(0, 48, 1));
        nav.addView(button("Bureaux", false, this::showEmploymentOffices), new LinearLayout.LayoutParams(0, 48, 1));
        nav.addView(button("Profil", false, this::showProfile), new LinearLayout.LayoutParams(0, 48, 1));
        root.addView(nav);
    }

    private void card(String title, String company, String location, String type, String description) {
        LinearLayout c = column();
        c.setPadding(17, 15, 17, 15);
        c.setBackground(shape(Color.WHITE, 18));
        c.addView(text(title, 18, ink, true));
        space(c, 5);
        c.addView(text(company + "  •  " + location, 13, muted, false));
        space(c, 10);
        c.addView(text(type, 12, green, true));
        space(c, 8);
        c.addView(text(description, 14, ink, false));
        space(c, 12);
        c.addView(button("Voir l'offre", true, () -> showJob(title, company, location, description)));
        content.addView(c);
        space(content, 12);
    }

    private void infoCard(String title, String detail, String actionLabel, Runnable action) {
        LinearLayout c = column();
        c.setPadding(17, 15, 17, 15);
        c.setBackground(shape(Color.WHITE, 18));
        c.addView(text(title, 18, ink, true));
        space(c, 8);
        c.addView(text(detail, 14, muted, false));
        space(c, 12);
        c.addView(button(actionLabel, true, action));
        content.addView(c);
        space(content, 12);
    }

    private Spinner filterSpinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private String selected(Spinner spinner) {
        Object value = spinner.getSelectedItem();
        return value == null ? "Tous" : value.toString();
    }

    private void renderJobs() {
        content.removeAllViews();
        String query = searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        String governorate = selected(governorateFilter);
        String sector = selected(sectorFilter);
        String contract = selected(contractFilter);

        content.addView(text("Résultats de démonstration", 20, ink, true));
        space(content, 5);
        content.addView(text("Ces exemples sont fictifs : aucune candidature réelle n'est possible pour le moment.", 12, muted, false));
        space(content, 12);

        int count = 0;
        for (Job job : demoJobs) {
            String searchable = (job.title + " " + job.company + " " + job.location + " "
                    + job.sector + " " + job.contract + " " + job.description).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !searchable.contains(query)) continue;
            if (!"Tous".equals(governorate) && !job.location.equalsIgnoreCase(governorate)) continue;
            if (!"Tous".equals(sector) && !job.sector.equalsIgnoreCase(sector)) continue;
            if (!"Tous".equals(contract) && !job.contract.equalsIgnoreCase(contract)) continue;
            card(job.title, job.company, job.location, job.sector + " • " + job.contract, job.description);
            count++;
        }
        if (count == 0) {
            content.addView(text("Aucune offre ne correspond à ces filtres.", 15, muted, false));
        } else {
            content.addView(text(count + " résultat(s)", 12, muted, false));
        }
    }

    private void showHome() {
        base("Trouvez votre prochain emploi", "Les opportunités près de chez vous, en Tunisie.");
        LinearLayout switcher = row();
        switcher.addView(button("Chercher un emploi", !employerMode, () -> { employerMode = false; showHome(); }),
                new LinearLayout.LayoutParams(0, 48, 1));
        switcher.addView(button("Recruter", employerMode, () -> { employerMode = true; showHome(); }),
                new LinearLayout.LayoutParams(0, 48, 1));
        content.addView(switcher);
        space(content, 12);
        content.addView(button("Trouver un bureau de l'emploi", false, this::showEmploymentOffices));
        space(content, 18);

        if (employerMode) {
            content.addView(text("Vous recrutez ?", 21, ink, true));
            space(content, 8);
            content.addView(text("Publiez une offre et aidez les talents de Kasserine et de toute la Tunisie à vous trouver.", 15, muted, false));
            space(content, 14);
            content.addView(button("Créer une offre (bientôt)", true, this::showNotice));
            return;
        }

        content.addView(text("Rechercher une offre", 20, ink, true));
        space(content, 8);
        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(15);
        searchInput.setHint("Métier, mot-clé ou entreprise");
        searchInput.setPadding(14, 10, 14, 10);
        searchInput.setBackground(shape(Color.WHITE, 14));
        content.addView(searchInput, new LinearLayout.LayoutParams(-1, 52));
        space(content, 10);

        content.addView(text("Gouvernorat", 13, muted, true));
        governorateFilter = filterSpinner(new String[] {
                "Tous", "Kasserine", "Tunis", "Ariana", "Ben Arous", "Manouba", "Nabeul",
                "Zaghouan", "Bizerte", "Béja", "Jendouba", "Le Kef", "Siliana", "Kairouan",
                "Sidi Bouzid", "Sousse", "Monastir", "Mahdia", "Sfax", "Gafsa", "Tozeur",
                "Kébili", "Gabès", "Médenine", "Tataouine"
        });
        content.addView(governorateFilter, new LinearLayout.LayoutParams(-1, 48));
        space(content, 6);

        content.addView(text("Secteur", 13, muted, true));
        sectorFilter = filterSpinner(new String[] {
                "Tous", "Agriculture", "Bâtiment", "Commerce", "Informatique", "Santé", "Transport"
        });
        content.addView(sectorFilter, new LinearLayout.LayoutParams(-1, 48));
        space(content, 6);

        content.addView(text("Type de contrat", 13, muted, true));
        contractFilter = filterSpinner(new String[] {
                "Tous", "CDI", "CDD", "Temps plein", "Saisonnier"
        });
        content.addView(contractFilter, new LinearLayout.LayoutParams(-1, 48));
        space(content, 12);

        LinearLayout filterActions = row();
        filterActions.addView(button("Rechercher", true, this::renderJobs),
                new LinearLayout.LayoutParams(0, 48, 1));
        filterActions.addView(button("Effacer", false, () -> {
            searchInput.setText("");
            governorateFilter.setSelection(0);
            sectorFilter.setSelection(0);
            contractFilter.setSelection(0);
            renderJobs();
        }), new LinearLayout.LayoutParams(0, 48, 1));
        content.addView(filterActions);
        space(content, 18);
        renderJobs();
    }

    private void officeCard(String governorate, String director, String address, String phone, String email) {
        LinearLayout c = column();
        c.setPadding(16, 14, 16, 14);
        c.setBackground(shape(Color.WHITE, 18));
        c.addView(text(governorate, 17, ink, true));
        space(c, 4);
        c.addView(text("Responsable : " + director, 13, green, true));
        space(c, 6);
        c.addView(text(address, 13, muted, false));
        c.addView(text("Téléphone : " + phone, 13, muted, false));
        if (email != null && !email.isEmpty()) {
            c.addView(text("E-mail : " + email, 13, muted, false));
        }
        space(c, 10);
        LinearLayout actions = row();
        actions.addView(button("Appeler", true, () -> openExternal("tel:" + phone.replace(" ", ""))),
                new LinearLayout.LayoutParams(0, 46, 1));
        if (email != null && !email.isEmpty()) {
            actions.addView(button("E-mail", false, () -> openExternal("mailto:" + email)),
                    new LinearLayout.LayoutParams(0, 46, 1));
        }
        c.addView(actions);
        content.addView(c);
        space(content, 10);
    }

    private void showEmploymentOffices() {
        base("Emploi dans les régions", "Directions régionales — les 24 gouvernorats.");
        content.addView(text("KASSERINE EN PREMIER", 13, green, true));
        space(content, 6);
        content.addView(text("Contacts régionaux officiels", 20, ink, true));
        space(content, 8);
        content.addView(text("Coordonnées publiées par le Ministère de l'Emploi et de la Formation professionnelle. Ces fiches concernent les directions régionales, pas les adresses individuelles de chaque bureau local. Page officielle consultée le 8 novembre 2026.", 13, muted, false));
        space(content, 14);

        String[][] regions = {
            {"Kasserine", "Mohamed Armi", "Rue 7 novembre, BP 111, 1200 Kasserine", "77 475 366", "Mohamed.Armi@mfpe.state.tn"},
            {"Tunis", "Sondes Gmir", "6, rue de Cologne, 1001 Tunis", "71 789 389", "sondes.gmir@mfpe.state.tn"},
            {"Ariana", "Faten Jerjini", "17 Bis, rue Ahmed Khabtheni, 2080 Ariana", "71 717 696", "faten.jerjini@mfpe.state.tn"},
            {"Ben Arous", "Adel Khelifi", "56, rue de la France, 2013 Ben Arous", "71 381 510", "adel.khelifi@mfpe.state.tn"},
            {"Manouba", "Mbarka Rgoubi", "5, rue Abou Taieb Almoutanebbi, 2010 Manouba", "70 604 133", "mbarka.ragoubi@mfpe.state.tn"},
            {"Nabeul", "Adel Zitouni", "33, rue de France, 8000 Nabeul", "72 285 913", "adel.zitouni@mfpe.state.tn"},
            {"Zaghouan", "Anis Lachheb", "Rue Narjess, 1100 Zaghouan", "72 675 588", "anis.lachheb@mfpe.state.tn"},
            {"Bizerte", "Hamouda Gharech", "169, rue Hassen Ennouri, 7000 Bizerte", "72 432 378", "hamouda.gharech@mfpe.state.tn"},
            {"Béja", "Hatem Raihani", "Avenue Habib Bourguiba, 9000 Béja", "78 456 177", "hatem.raihani@mfpe.state.tn"},
            {"Jendouba", "Mohamed Rahali", "9, avenue 9 Avril, 8100 Jendouba", "78 601 160", "mohamed.rahali@mfpe.state.tn"},
            {"Le Kef", "Jalel Twihri", "Rue Taha Hassine, 7120 Le Kef", "78 223 822", "jalel.twihri@mfpe.state.tn"},
            {"Siliana", "Atef Bouraoui", "Rue 31 Août, 6100 Siliana", "78 872 580", "atef.bouraoui@mfpe.state.tn"},
            {"Kairouan", "Abdellatif Abbassi", "16, rue Baiet Elhikma, 3100 Kairouan", "77 230 876", "abdellatif.abbassi@mfpe.state.tn"},
            {"Sidi Bouzid", "Aymen Boualagui", "51, rue Karra Hdid, 9100 Sidi Bouzid", "76 633 545", "aymen.bouallagui@mfpe.state.tn"},
            {"Sousse", "Tarek Mahjoub", "Rue Ksantina, 4003 Sousse", "73 221 600", "tarek.mahjoub@mfpe.state.tn"},
            {"Monastir", "Sawssen Touir", "Rue Ibn Sina, 5000 Monastir", "73 461 866", "sawssen.touir@mfpe.state.tn"},
            {"Mahdia", "Khaled Hadj Hassen", "Rue Bachir Sfar, BP 111, 5100 Mahdia", "73 696 421", ""},
            {"Sfax", "Noura Zidi", "Rue Jemelleddine Afghani, Bouderiar 1, Sfax", "74 222 592", "noura.zidi@mfpe.state.tn"},
            {"Gafsa", "Mohamed Sniha", "Rue Erroman Douali, 2100 Gafsa", "76 228 346", "sniha.mohamed@mfpe.state.tn"},
            {"Tozeur", "Omar Khmira", "Route de Nafta, 2200 Tozeur", "76 468 631", "omar.khmira@mfpe.state.tn"},
            {"Kébili", "Othman Mahmoudi", "Route de Gabès, 4200 Kébili", "75 490 682", "othman.mahmoudi@mfpe.state.tn"},
            {"Gabès", "Nizar Khaldi", "Rue Belkacem Bazmi, 6000 Gabès", "75 270 435", "nizar.khaldi@mfpe.state.tn"},
            {"Médenine", "Mchabet Meftah", "Rue Borma, 4100 Médenine", "75 641 915", "meftah.mchabet@mfpe.state.tn"},
            {"Tataouine", "Kilani Oun", "Avenue El Koufa, cité El Mahrajen, Tataouine", "75 844 994", "kilani.jabri@mfpe.state.tn"}
        };
        for (String[] region : regions) {
            officeCard(region[0], region[1], region[2], region[3], region[4]);
        }

        space(content, 8);
        content.addView(text("Sources et actualisation", 18, ink, true));
        space(content, 6);
        content.addView(text("Source principale : annuaire officiel des directions régionales du ministère. Les coordonnées peuvent changer ; vérifiez-les auprès du ministère avant un déplacement. Pour Kasserine, la page ministérielle indique 3 bureaux d'emploi, mais cette liste ne prétend pas fournir leurs trois adresses individuelles.", 13, muted, false));
        space(content, 10);
        content.addView(button("Ouvrir l'annuaire officiel du ministère", true, () -> openExternal(MINISTRY_DIRECTORY)));
        space(content, 16);
        content.addView(text("Jeu de données national complémentaire", 17, ink, true));
        space(content, 5);
        content.addView(text("Le catalogue national propose un fichier CSV et XLSX sous licence CC BY. Attention : les ressources sont datées du 7 mars 2023 et leur couverture temporelle indiquée est 2016–2019. Elles ne sont pas utilisées ici comme source de coordonnées actuelles sans revalidation.", 13, muted, false));
        space(content, 10);
        content.addView(button("Ouvrir le jeu de données officiel", false, () -> openExternal(OPEN_DATA_DIRECTORY)));
    }

    private void showProfile() {
        base("Votre espace", "Un profil simple pour préparer votre recherche.");
        content.addView(text("Candidat ou recruteur", 20, ink, true));
        space(content, 8);
        content.addView(text("La création de compte, l'ajout de photo, le CV et le numéro de téléphone seront activés avec un système sécurisé.", 15, muted, false));
        space(content, 18);
        content.addView(button("Je cherche un emploi", true, () -> { employerMode = false; showHome(); }));
        space(content, 10);
        content.addView(button("Je propose un emploi", false, () -> { employerMode = true; showHome(); }));
        space(content, 20);
        content.addView(text("Confidentialité d'abord", 17, ink, true));
        content.addView(text("Vos coordonnées et votre CV ne seront pas publiés sans votre accord. Les profils et les offres devront être protégés contre les faux comptes et les arnaques.", 14, muted, false));
    }

    private void showJob(String title, String company, String location, String description) {
        base(title, company + " • " + location);
        content.addView(text(description, 16, ink, false));
        space(content, 18);
        content.addView(button("Postuler (bientôt)", true, this::showNotice));
        space(content, 10);
        content.addView(button("Retour aux offres", false, this::showHome));
    }

    private void showNotice() {
        base("Fonction en préparation", "Cette première version est une base de démonstration.");
        content.addView(text("Les candidatures, comptes et publications seront ajoutés après la mise en place du backend sécurisé.", 15, muted, false));
        space(content, 14);
        content.addView(button("Retour", true, this::showHome));
    }
}
