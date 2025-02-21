package org.hl7.fhir.tools.site;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.utilities.FileUtilities;
import org.hl7.fhir.utilities.Utilities;
import org.hl7.fhir.utilities.json.JsonTrackingParser;
import org.hl7.fhir.utilities.json.JsonUtilities;

import com.google.gson.JsonObject;

public class WebSiteMaintainer {

    public static void main(String[] args) throws IOException {
        String root = FileUtilities.getDirectoryForFile(args[0]);
        List<JsonObject> pubs = new ArrayList<>();
        List<String> dirs = new ArrayList<>();
        JsonObject cv = null;
        JsonObject pl = JsonTrackingParser.parseJsonFile(args[0]);
        for (JsonObject v : JsonUtilities.objects(pl, "list")) {
            String ver = JsonUtilities.str(v, "version");
            if (!"current".equals(ver)) {
                String p = JsonUtilities.str(v, "path").substring(20);
                v.addProperty("directory", Utilities.path(root, p));
                dirs.add(Utilities.path(root, p));
                pubs.add(v);
                if (v.has("current") && v.get("current").getAsBoolean())
                    cv = v;
            }
        }
        for (JsonObject v : pubs) {
            new WebSiteReleaseUpdater(root, JsonUtilities.str(v, "directory"), v, cv).execute(null);
        }
        new WebSiteReleaseUpdater(root, root, cv, cv).execute(dirs);
    }

}
