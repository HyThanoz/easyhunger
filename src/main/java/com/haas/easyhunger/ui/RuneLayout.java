package com.haas.easyhunger.ui;

import com.haas.easyhunger.EasyHunger;
import com.haas.easyhunger.config.EasyHungerConfig;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;

/**
 * Geometria de los HUD de hambre y sed (1.4.0; reescrita en 1.5.20).
 *
 * Vive aparte porque los dos HUDs necesitan exactamente los mismos numeros y antes estaban
 * duplicados literalmente en {@code EasyHungerHud} y {@code EasyWaterHud}.
 *
 * Desde 1.5.20: los anillos se movian segun la resolucion porque iban pegados al
 * borde DERECHO de la pantalla mientras el hotbar va centrado. Ahora todo se mide desde el CENTRO:
 * la raiz de cada .ui lleva solo Bottom + Width + Height y el cliente la centra (como Health.ui y
 * el hotbar), asi que la distancia al hotbar es siempre la misma.
 * <ul>
 *   <li>ANILLOS: a la derecha del hotbar, en espejo del espacio Z (Hotbar.ui del cliente 0.7.1:
 *       el espacio Z va en Left -28-74 del hotbar de 702, o sea su cara visible termina 26 antes
 *       del primer espacio; aqui el primer anillo empieza 26 despues del ultimo).</li>
 *   <li>BARRAS: encima de la vida (hambre) y de la stamina (sed), con su misma anatomia. Toda la
 *       geometria de las barras esta en HungerBar.ui y WaterBar.ui; aqui solo la altura.</li>
 * </ul>
 */
public final class RuneLayout {

    /**
     * Lado de la CAJA del anillo. Tiene que coincidir con el Anchor de Hunger.ui y Water.ui.
     *
     * Es mayor que la moneda a proposito y no se puede encoger: el relleno del
     * CircularProgressBar es un cuadrado girado inscrito en la caja, que en las diagonales solo
     * llega al 70.7% del semilado. El aro tiene que caber dentro de ese rombo o salen cuatro
     * cunas en cruz en lugar de un aro. Los 18 px de borde son transparentes y existen solo para
     * que el rombo tape el aro.
     */
    public static final int BOX = 100;

    /** Diametro de la moneda que se ve de verdad dentro de la caja. */
    public static final int COIN = 64;

    /** Borde transparente a cada lado. Hay que descontarlo al posicionar. */
    public static final int MARGIN = (BOX - COIN) / 2;

    /** Aire VISIBLE entre las dos monedas (no entre cajas). */
    public static final int GAP = 8;

    // Constantes del hotbar vanilla, tomadas de Hotbar.ui y Common.ui del cliente 0.7.1.
    private static final int SLOT_SIZE = 74;
    private static final int GRID_PADDING = 2;
    private static final int SLOT_SPACING = 4;
    private static final int HOTBAR_WIDTH = 9 * SLOT_SIZE + 9 * SLOT_SPACING;          // 702
    private static final int HOTBAR_HEIGHT = SLOT_SIZE + (2 * GRID_PADDING);           // 78
    private static final int HOTBAR_BOTTOM = 30 + 6;                                   // InventoryClosedContainerMargin
    /** Borde derecho visible del ultimo espacio, medido desde el borde izquierdo del hotbar. */
    private static final int HOTBAR_VISIBLE_RIGHT = GRID_PADDING + 9 * SLOT_SIZE + 8 * SLOT_SPACING; // 700
    /** Aire entre la cara visible del espacio Z (-100..-26) y la del primer espacio (que empieza en 2). */
    private static final int Z_GAP = 26 + GRID_PADDING;                                // 28

    /** Izquierda de la primera moneda, medida desde el CENTRO de la pantalla (espejo del Z). */
    private static final int COIN_LEFT_FROM_CENTER = -HOTBAR_WIDTH / 2 + HOTBAR_VISIBLE_RIGHT + Z_GAP; // 377
    /** Media anchura de la raiz de los anillos: llega hasta el borde de la caja de sed. */
    private static final int RING_HALF = COIN_LEFT_FROM_CENTER + COIN + GAP + COIN + MARGIN;                   // 531
    /** Centro vertical del hotbar, desde abajo. */
    private static final int HOTBAR_MID = HOTBAR_BOTTOM + HOTBAR_HEIGHT / 2;           // 75

    /** Fila de las barras (la vida nativa va en 120 y su icono sube hasta 140). */
    private static final int BAR_ROOT_BOTTOM = 142;
    private static final int BAR_ROOT_HEIGHT = 24;

    private RuneLayout() {
    }

    /** .ui del HUD segun el estilo de la config (mismos ids en las dos variantes). */
    public static String uiFile(boolean isHunger) {
        boolean barras = EasyHunger.get().getConfig().isHudBarras();
        if (isHunger) return barras ? "HUD/Hunger/HungerBar.ui" : "HUD/Hunger/Hunger.ui";
        return barras ? "HUD/Hunger/WaterBar.ui" : "HUD/Hunger/Water.ui";
    }

    /**
     * Ancla de la RAIZ: sin Left ni Right, el cliente la centra. {@code HudOffsetY} sube (+) o baja (-).
     */
    public static Anchor rootAnchor() {
        EasyHungerConfig config = EasyHunger.get().getConfig();
        int offsetY = config.getHudOffsetY();
        Anchor anchor = new Anchor();
        if (config.isHudBarras()) {
            anchor.setWidth(Value.of(HOTBAR_WIDTH));
            anchor.setHeight(Value.of(BAR_ROOT_HEIGHT));
            anchor.setBottom(Value.of(BAR_ROOT_BOTTOM + offsetY));
        } else {
            anchor.setWidth(Value.of(2 * RING_HALF));
            anchor.setHeight(Value.of(BOX));
            anchor.setBottom(Value.of(HOTBAR_MID - BOX / 2 + offsetY));
        }
        return anchor;
    }

    /**
     * Ancla de la caja del anillo dentro de la raiz (hambre primero, sed a su derecha). Con BARRAS
     * devuelve null: su geometria vive en el .ui.
     */
    public static Anchor containerAnchor(boolean isHunger) {
        if (EasyHunger.get().getConfig().isHudBarras()) return null;
        int coinLeft = RING_HALF + COIN_LEFT_FROM_CENTER + (isHunger ? 0 : COIN + GAP);
        Anchor anchor = new Anchor();
        anchor.setLeft(Value.of(coinLeft - MARGIN));
        anchor.setTop(Value.of(0));
        anchor.setWidth(Value.of(BOX));
        anchor.setHeight(Value.of(BOX));
        return anchor;
    }
}
