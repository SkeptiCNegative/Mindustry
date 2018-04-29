package io.anuke.mindustry.graphics;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Disposable;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.game.EventType.TileChangeEvent;
import io.anuke.mindustry.game.EventType.WorldLoadEvent;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Events;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Pixmaps;
import io.anuke.ucore.util.Mathf;

import static io.anuke.mindustry.Vars.*;

public class MinimapRenderer implements Disposable{
    private static final int baseSize = 16;
    private Pixmap pixmap;
    private Texture texture;
    private TextureRegion region;
    private Rectangle rect = new Rectangle();
    private int zoom = 4;

    public MinimapRenderer(){
        Events.on(WorldLoadEvent.class, () -> {
            reset();
            updateAll();
        });

        Events.on(TileChangeEvent.class, this::update);
    }

    public Texture getTexture(){
        return texture;
    }

    public void zoomBy(int amount){
        zoom += amount;
        zoom = Mathf.clamp(zoom, 1, Math.min(world.width(), world.height())/baseSize/2);
    }

    public void reset(){
        if(pixmap != null){
            pixmap.dispose();
            texture.dispose();
        }
        pixmap = new Pixmap(world.width(), world.height(), Format.RGBA8888);
        texture = new Texture(pixmap);
        region = new TextureRegion(texture);
    }

    public void drawEntities(float x, float y, float w, float h){
        int sz = baseSize * zoom;
        float dx = (Core.camera.position.x / tilesize);
        float dy = (Core.camera.position.y / tilesize);
        dx = Mathf.clamp(dx, sz, world.width()-sz);
        dy = Mathf.clamp(dy, sz, world.height()-sz);

        rect.set((dx - sz) * tilesize, (dy - sz) * tilesize, sz * 2 * tilesize, sz * 2 * tilesize);
        Units.getNearby(rect, unit -> {
            if(!rect.contains(unit.x, unit.y)) return;

            float rx = (unit.x - rect.x) / rect.width * w, ry = (unit.y - rect.y)/ rect.width * h;
            Draw.color(unit.team.color);
            Draw.rect("white", x + rx, y + ry, w/(sz*2), h/(sz*2));
        });
        Draw.color();
    }

    public TextureRegion getRegion() {
        int sz = Mathf.clamp(baseSize * zoom, baseSize, Math.min(world.width(), world.height()));
        float dx = (Core.camera.position.x / tilesize);
        float dy = (Core.camera.position.y / tilesize);
        dx = Mathf.clamp(dx, sz, world.width()-sz);
        dy = Mathf.clamp(dy, sz, world.height()-sz);
        float invTexWidth = 1f / texture.getWidth();
        float invTexHeight = 1f / texture.getHeight();
        float x = dx - sz, y = world.height()-dy - sz, width = sz*2, height = sz*2;
        region.setRegion(x * invTexWidth, y * invTexHeight, (x + width) * invTexWidth, (y + height) * invTexHeight);
        return region;
    }

    public void updateAll(){
        for(int x = 0; x < world.width(); x ++){
            for(int y = 0; y < world.height(); y ++){
                pixmap.drawPixel(x, pixmap.getHeight() - 1 - y, colorFor(world.tile(x, y)));
            }
        }
        texture.draw(pixmap, 0, 0);
    }

    public void update(Tile tile){
        int color = colorFor(world.tile(tile.x, tile.y));
        pixmap.drawPixel(tile.x, pixmap.getHeight() - 1 - tile.y, color);

        texture.bind();
        Pixmaps.drawPixel(texture, tile.x, pixmap.getHeight() - 1 - tile.y, color);
    }

    private int colorFor(Tile tile){
        int color = tile.breakable() ? tile.target().getTeam().intColor : ColorMapper.getColor(tile.block());
        if(color == 0) color = ColorMapper.getColor(tile.floor());
        return color;
    }

    @Override
    public void dispose() {
        pixmap.dispose();
        texture.dispose();
        texture = null;
        pixmap = null;
    }
}