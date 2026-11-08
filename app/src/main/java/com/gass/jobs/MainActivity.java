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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private final int ink = Color.rgb(22, 35, 52);
    private final int muted = Color.rgb(104, 119, 135);
    private final int green = Color.rgb(0, 145, 105);
    private final int canvas = Color.rgb(246, 248, 250);
    private LinearLayout root;
    private LinearLayout content;
    private boolean employerMode = false;

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

        content.addView(text("Offres à découvrir", 21, ink, true));
        space(content, 5);
        content.addView(text("Exemples de démonstration — les offres ne sont pas encore vérifiées.", 12, muted, false));
        space(content, 14);
        card("Agent commercial", "Entreprise de démonstration", "Kasserine", "Temps plein",
                "Relation client, prospection et suivi commercial. Expérience souhaitée.");
        card("Technicien informatique", "Entreprise de démonstration", "Sbeitla", "CDI",
                "Assistance informatique, maintenance et support aux utilisateurs.");
        card("Ouvrier agricole", "Exploitation de démonstration", "Fériana", "Saisonnier",
                "Travaux agricoles. Débutants acceptés, formation sur place.");
    }

    private void showEmploymentOffices() {
        base("Bureaux de l'emploi", "Contacts officiels — Kasserine en premier.");
        content.addView(text("KASSERINE", 13, green, true));
        space(content, 6);
        content.addView(text("Direction régionale de la formation professionnelle et de l'emploi", 20, ink, true));
        space(content, 8);
        content.addView(text("La page officielle du ministère indique 3 bureaux d'emploi dans le gouvernorat de Kasserine. Les coordonnées ci-dessous sont celles de la direction régionale, et non les adresses individuelles des trois bureaux.", 14, muted, false));
        space(content, 14);

        infoCard("Direction régionale — Kasserine",
                "Directeur régional : Mohamed Armi\nAdresse : Rue 7 Novembre, BP 111, 1200 Kasserine\nTéléphone : 77 475 366\nE-mail : Mohamed.Armi@mfpe.state.tn\n\nSource officielle consultée le 8 novembre 2026.",
                "Consulter la page officielle", () -> openExternal(MINISTRY_DIRECTORY));

        content.addView(text("Réseau national des bureaux", 19, ink, true));
        space(content, 6);
        content.addView(text("Le catalogue national des données ouvertes propose un répertoire des bureaux en CSV et XLSX sous licence CC BY. Attention : les ressources sont datées du 7 mars 2023 et la période couverte indiquée est 2016–2019. Elles ne doivent pas être présentées comme des coordonnées actuelles sans vérification.", 14, muted, false));
        space(content, 12);
        content.addView(button("Ouvrir le jeu de données officiel", false, () -> openExternal(OPEN_DATA_DIRECTORY)));
        space(content, 18);
        content.addView(text("À propos des données", 16, ink, true));
        space(content, 5);
        content.addView(text("GASS-JOBS affiche la source et la date de consultation. Les coordonnées régionales doivent être revalidées avant utilisation. Les trois bureaux locaux de Kasserine seront ajoutés séparément après vérification de leurs adresses et numéros.", 13, muted, false));
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
