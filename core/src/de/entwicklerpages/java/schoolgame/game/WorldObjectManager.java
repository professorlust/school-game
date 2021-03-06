package de.entwicklerpages.java.schoolgame.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.List;

import de.entwicklerpages.java.schoolgame.SchoolGame;
import de.entwicklerpages.java.schoolgame.game.objects.*;

/**
 * Verwaltet alle Ingame Objekte wie
 * <ul>
 *     <li>Gegner</li>
 *     <li>Items</li>
 *     <li>Trigger Zonen</li>
 *     <li>statische Objekte (Türen, NPCs)</li>
 * </ul>
 *
 * @author nico
 */
public class WorldObjectManager implements Disposable
{
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////// EIGENSCHAFTEN ////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private WorldObjectConfig config;
    private List<WorldObject> worldObjects;
    private Array<Interactable> interactionObjects;
    private Array<Attackable> attackObjects;
    private Array<UpdateObject> updateObjects;
    private final SchoolGame game;
    private final World physicalWorld;
    private ExtendedOrthogonalTiledMapRenderer renderer;
    private AttackCallback attackPlayerCallback = null;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////// METHODEN /////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Erstellt einen neuen WorldObjectManager.
     *
     * @param game Zugriff auf die Spielinstanz
     * @param physicalWorld Zugriff auf die Box2D Welt
     */
    public WorldObjectManager(SchoolGame game, World physicalWorld)
    {
        this.game = game;
        this.physicalWorld = physicalWorld;

        worldObjects = new ArrayList<WorldObject>();
        interactionObjects = new Array<Interactable>(false, 5);
        attackObjects = new Array<Attackable>(false, 8);
        updateObjects = new Array<UpdateObject>(false, 10);
    }

    public void setAttackPlayerCallback(AttackCallback attackPlayerCallback)
    {
        this.attackPlayerCallback = attackPlayerCallback;
    }

    /**
     * Initialisiert ein Tiled MapObject.
     *
     * @param objectId die ID des Objektes
     * @param object Zugriff auf das Objekt selbst
     */
    public void initObject(String objectId, MapObject object)
    {
        if (config == null)
        {
            Gdx.app.error("ERROR", "No world config was created!");
            return;
        }

        for (WorldObject worldObject : config.getWorldObjects())
        {
            if (worldObject.getObjectId().equals(objectId))
            {
                worldObject.setWorldObjectManager(this);
                worldObject.setRawObject(object);
                worldObject.onInit();

                if (worldObject instanceof UpdateObject)
                {
                    updateObjects.add((UpdateObject) worldObject);
                }

                worldObjects.add(worldObject);
                config.getWorldObjects().remove(worldObject);
                return;
            }
        }

        Gdx.app.log("WARNING", "Object '" + objectId + "' is not a part of the level!");
    }

    /**
     * Sperrt die Konfiguration.
     */
    public void lockConfig()
    {
        config.lockConfig();
    }

    /**
     * Schließt die Initialisierung ab.
     * Sollten Objekte nicht in der TiledMap gefunden worden sein, wird eine Warnung ausgegeben.
     */
    public void finishInit(ExtendedOrthogonalTiledMapRenderer renderer)
    {
        if (config == null)
        {
            Gdx.app.error("ERROR", "No world config was created!");
            return;
        }

        this.renderer = renderer;

        if (!config.getWorldObjects().isEmpty())
        {
            for (WorldObject object : config.getWorldObjects())
            {
                Gdx.app.log("WARNING", "Object '" + object.getObjectId() + "' was not found in the map!");
            }
        }

        for (WorldObject object : worldObjects)
        {
            if (object instanceof ExtendedMapDisplayObject)
            {
                renderer.addDisplayObject((ExtendedMapDisplayObject) object);
            }
        }

        Gdx.app.log("INFO", "World is ready.");
    }

    /**
     * Wird bei jedem Frame aufgerufen.
     *
     * @param deltaTime die Zeit, die seit dem letzten Frame vergangen ist
     */
    public void update(float deltaTime)
    {
        for (UpdateObject object : updateObjects)
        {
            object.onUpdate(deltaTime);
        }
    }

    /**
     * Aufräumarbeiten.
     *
     * Alle Objekte werden informiert, das sie ihre Resourcen aufräumen sollen.
     */
    @Override
    public void dispose()
    {
        for (WorldObject object : worldObjects)
        {
            object.onDispose();
        }
    }

    /**
     * Erzeugt eine neue Konfiguration.
     *
     * @see WorldObjectConfig
     *
     * @return die leere Konfiguration
     */
    public WorldObjectConfig createConfig()
    {
        if (config == null)
        {
            config = new WorldObjectConfig();
        }
        return config;
    }

    /**
     * Reagiert auf Spielerinteraktionen.
     *
     * @param player Zugriff auf die Spielerinstanz (Position)
     */
    public void playerInteraction(Player player)
    {
        Interactable mostImportant = null;
        int highestPriority = 0;

        for (Interactable interaction : interactionObjects)
        {
            if (interaction.getInteractionPriority() >= highestPriority)
            {
                mostImportant = interaction;
                highestPriority = interaction.getInteractionPriority();
            }
        }

        if (mostImportant != null)
            mostImportant.interaction(player);
    }

    /**
     * Setzt ein Objekt, das bei einer Spielerinteraktion *sofort* aufgerufen werden soll.
     * Werden mehrere Objekte hinzugefügt, wird nur das Objekt mit der höchsten Priorität aufgerufen.
     *
     * @param interactable das Objekt, das gesetzt werden soll
     */
    public void registerForInteraction(Interactable interactable)
    {
        if (!interactionObjects.contains(interactable, true))
            interactionObjects.add(interactable);
    }

    /**
     * Beachtet das Objekt nicht mehr bei Spielerinteraktionen.
     *
     * @param interactable das Objekt, das nicht mehr beachtet werden soll
     */
    public void unregisterForInteraction(Interactable interactable)
    {
        interactionObjects.removeValue(interactable, true);
    }

    /**
     * Zeigt, ob der Spieler mit etwas interagieren kann.
     *
     * @return true, wenn der Spieler mit etwas interagieren kann
     */
    public boolean interactionPossible()
    {
        return interactionObjects.size > 0;
    }

    /**
     * Reagiert auf Spielerangriffe.
     *
     * @param player Zugriff auf die Spielerinstanz
     */
    public void playerAttack(Player player, int damage)
    {
        for (Attackable attackObject : attackObjects)
        {
            if (attackObject.onPlayerAttack(player, damage))
                break;
        }
    }

    /**
     * Setzt ein Objekt, das bei einer Spielerattacke *sofort* schaden nehmen soll.
     * Werden mehrere Objekte hinzugefügt, bekommt nur eins den Schaden.
     *
     * @param attackable das Objekt, das gesetzt werden soll
     */
    public void registerForAttack(Attackable attackable)
    {
        if (!attackObjects.contains(attackable, true))
            attackObjects.add(attackable);
    }

    /**
     * Beachtet das Objekt nicht mehr bei Spielerangriffen.
     *
     * @param attackable das Objekt, das nicht mehr beachtet werden soll
     */
    public void unregisterForAttack(Attackable attackable)
    {
        attackObjects.removeValue(attackable, true);
    }

    /**
     * Führt einen Angriff auf den Spieler durch.
     * @param damage
     */
    public void attackPlayer(int damage)
    {
        if (attackPlayerCallback == null)
        {
            Gdx.app.log("WARNING", "No damage can be applied to the player -> missing callback!");
            return;
        }

        attackPlayerCallback.run(damage);
    }

    /**
     * Entfernt ein Objekt aus der Welt
     *
     * @param object das Objekt, das entfernt werden soll
     */
    public void removeObject(WorldObject object)
    {
        if (object instanceof UpdateObject && updateObjects.contains((UpdateObject) object, true))
            updateObjects.removeValue((UpdateObject) object, true);

        if (object instanceof ExtendedMapDisplayObject)
            renderer.removeDisplayObject((ExtendedMapDisplayObject) object);

        object.onDispose();
        worldObjects.remove(object);
    }

    /**
     * Erlaubt vorallem Welt Objekten den Zugriff auf die Spielinstanz.
     *
     * @return die Spielinstanz
     */
    public SchoolGame getGame()
    {
        return game;
    }

    /**
     * Erlaubt vorallem Welt Objekten den Zugriff auf die Box2D Welt.
     *
     * @return die Box2D Welt
     */
    public World getPhysicalWorld()
    {
        return physicalWorld;
    }

    /**
     * Speichert die Liste der Welt Objekte, die in der TiledMap vorkommen sollten.
     *
     * @author nico
     */
    public class WorldObjectConfig
    {
        private List<WorldObject> worldObjects = new ArrayList<WorldObject>();
        private boolean locked;

        /**
         * Privater Konstruktor.
         * Nur der WorldObjectManager darf Konfigurationen erstellen.
         *
         * @see WorldObjectManager#createConfig()
         */
        private WorldObjectConfig()
        {
            locked = false;
        }

        /**
         * Ruft die Liste der Weltobjekte ab.
         * Zugriff nur für den WorldObjectManager.
         *
         * @return die Liste der Weltobjekte
         */
        private List<WorldObject> getWorldObjects()
        {
            return worldObjects;
        }

        /**
         * Sperrt die Konfiguration.
         * Nach dem Aufruf können keine Objekte mehr hinzugefügt werden.
         */
        private void lockConfig()
        {
            locked = true;
        }

        /**
         * Fügt ein Objekt zur Welt hinzu.
         * Es muss auch in der TiledMap existieren.
         *
         * @param worldObject das neue Objekt
         */
        public void registerObject(WorldObject worldObject)
        {
            if (locked)
            {
                Gdx.app.error("ERROR", "Tried to add object after the config was locked!");
                return;
            }

            worldObjects.add(worldObject);
        }
    }

    /**
     * Ein spezielles Callback für Angriffe auf den Spieler.
     * Bei einem Aufruf wird der Schaden übermittelt.
     *
     * @author nico
     */
    public interface AttackCallback
    {
        void run(int damage);
    }
}
