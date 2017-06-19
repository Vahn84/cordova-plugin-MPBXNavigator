package com.vahn.cordova.mpbxnavigator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.StepManeuver;

import java.util.HashMap;
import java.util.Map;

public class Utils {
  private static Map<String,String> translation;
  /**
   * <p>
   * Returns the Mapbox access token set in the app resources.
   * </p>
   * It will first search for a token in the Mapbox object. If not found it
   * will then attempt to load the access token from the
   * {@code res/values/dev.xml} development file.
   *
   * @param context The {@link Context} of the {@link android.app.Activity} or {@link android.app.Fragment}.
   * @return The Mapbox access token or null if not found.
   */
  public static String getMapboxAccessToken(@NonNull Context context) {
    try {
      // Read out AndroidManifest
      String token = Mapbox.getAccessToken();
      if (token == null || token.isEmpty()) {
        throw new IllegalArgumentException();
      }
      return token;
    } catch (Exception exception) {
      // Use fallback on string resource, used for development
      int tokenResId = context.getResources()
        .getIdentifier("mapbox_access_token", "string", context.getPackageName());
      return tokenResId != 0 ? context.getString(tokenResId) : null;
    }
  }

  /**
   * Demonstrates converting any Drawable to an Icon, for use as a marker icon.
   */
  public static Icon drawableToIcon(@NonNull Context context, @DrawableRes int id) {
    Drawable vectorDrawable = ResourcesCompat.getDrawable(context.getResources(), id, context.getTheme());
    Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
      vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    vectorDrawable.draw(canvas);
    return IconFactory.getInstance(context).fromBitmap(bitmap);
  }

  public static void initializeTranslationArray() {

    translation = new HashMap<String, String>();

    translation.put("turn", "gira a");
    translation.put("use lane", "continua sulla corsia di");
    translation.put("on ramp", "entra in");
    translation.put("off ramp", "esci da");
    translation.put("fork", "mantieni la");
    translation.put("continue", "continua su");
    translation.put("right", "destra verso");
    translation.put("sharp right", "destra verso");
    translation.put("slight right", "destra verso");
    translation.put("left", "sinistra verso");
    translation.put("sharp left", "sinistra verso");
    translation.put("slight left", "sinistra verso");
    translation.put("straight", "dritto");
    translation.put("depart", "vai a");
    translation.put("arrive", "arrivato a destinazione");

  }

  public static String translate(LegStep step){

    StepManeuver man = step.getManeuver();
    String type = translation.get(man.getType());
    String modifier = translation.get(man.getModifier());
    String name = step.getName();

    if(type == null){
      return "";
    }

    if(modifier == null){
      return "";
    }

    if(name == null){
      return "";
    }

    Log.d("ORIGINAL", man.getType()+" "+man.getModifier()+" "+step.getName());

    String translated = type +" "+modifier+" "+name;
    return translated;
  }
}
