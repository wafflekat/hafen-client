/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static haven.KeyBinder.*;

public class Fightsess extends Widget {
    public static final Text.Foundry fnd = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), 14);
    public static final Tex cdframe = Resource.loadtex("gfx/hud/combat/cool");
    public static final Tex actframe = Buff.frame;
    public static final Coord actframeo = Buff.imgoff;
    public static final Tex indframe = Resource.loadtex("gfx/hud/combat/indframe");
    public static final Coord indframeo = (indframe.sz().sub(32, 32)).div(2);
    public static final Tex indbframe = Resource.loadtex("gfx/hud/combat/indbframe");
    public static final Coord indbframeo = (indframe.sz().sub(32, 32)).div(2);
    public static final Tex useframe = Resource.loadtex("gfx/hud/combat/lastframe");
    public static final Coord useframeo = (useframe.sz().sub(32, 32)).div(2);
    public static final int actpitch = 50;
    public static final KeyBinder.KeyBind[] keybinds = new KeyBinder.KeyBind[]{
	new KeyBinder.KeyBind(KeyEvent.VK_1, 0),
	new KeyBinder.KeyBind(KeyEvent.VK_2, 0),
	new KeyBinder.KeyBind(KeyEvent.VK_3, 0),
	new KeyBinder.KeyBind(KeyEvent.VK_4, 0),
	new KeyBinder.KeyBind(KeyEvent.VK_5, 0),
	new KeyBinder.KeyBind(KeyEvent.VK_1, SHIFT),
	new KeyBinder.KeyBind(KeyEvent.VK_2, SHIFT),
	new KeyBinder.KeyBind(KeyEvent.VK_3, SHIFT),
	new KeyBinder.KeyBind(KeyEvent.VK_4, SHIFT),
	new KeyBinder.KeyBind(KeyEvent.VK_5, SHIFT),
    };
    public final Action[] actions;
    public int use = -1, useb = -1;
    public Coord pcc;
    public int pho;
    private Fightview fv;

    public static class Action {
	public final Indir<Resource> res;
	public double cs, ct;

	public Action(Indir<Resource> res) {
	    this.res = res;
	}
    }

    @RName("fsess")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    int nact = (Integer)args[0];
	    return(new Fightsess(nact));
	}
    }

    @SuppressWarnings("unchecked")
    public Fightsess(int nact) {
	pho = -40;
	this.actions = new Action[nact];
    }

    protected void added() {
	fv = parent.getparent(GameUI.class).fv;
	presize();
	ui.gui.calendar.hide();
    }

    public void presize() {
	resize(parent.sz);
	pcc = sz.div(2);
    }
    
    @Override
    public void destroy() {
        ui.gui.calendar.show();
	if(CFG.CLEAR_PLAYER_DMG_AFTER_COMBAT.get()) {
	    haven.Action.CLEAR_PLAYER_DAMAGE.run(ui.gui);
	}
	if(CFG.CLEAR_ALL_DMG_AFTER_COMBAT.get()) {
	    haven.Action.CLEAR_ALL_DAMAGE.run(ui.gui);
	}
	super.destroy();
    }
    
    private void updatepos() {
	MapView map;
	Gob pl;
	if(((map = getparent(GameUI.class).map) == null) || ((pl = map.player()) == null) || (pl.sc == null))
	    return;
	pcc = pl.sc;
	pho = (int)(pl.sczu.mul(20f).y) - 20;
    }

    private static final Resource tgtfx = Resource.local().loadwait("gfx/hud/combat/trgtarw");
    private final Map<Pair<Long, Resource>, Sprite> cfx = new CacheMap<Pair<Long, Resource>, Sprite>();
    private final Collection<Sprite> curfx = new ArrayList<Sprite>();

    private void fxon(long gobid, Resource fx) {
	MapView map = getparent(GameUI.class).map;
	Gob gob = ui.sess.glob.oc.getgob(gobid);
	if((map == null) || (gob == null))
	    return;
	Pair<Long, Resource> id = new Pair<Long, Resource>(gobid, fx);
	Sprite spr = cfx.get(id);
	if(spr == null)
	    cfx.put(id, spr = Sprite.create(null, fx, Message.nil));
	map.drawadd(gob.loc.apply(spr));
	curfx.add(spr);
    }

    public void tick(double dt) {
	for(Sprite spr : curfx)
	    spr.tick((int)(dt * 1000));
	curfx.clear();
    }

    private static final Text.Furnace ipf = new PUtils.BlurFurn(new Text.Foundry(Text.serif, 18, new Color(128, 128, 255)).aa(true), 1, 1, new Color(48, 48, 96));
    private final Text.UText<?> ip = new Text.UText<Integer>(ipf) {
	public String text(Integer v) {return(CFG.ALT_COMBAT_UI.get()?v.toString():"IP: " + v);}
	public Integer value() {return(fv.current.ip);}
    };
    private final Text.UText<?> oip = new Text.UText<Integer>(ipf) {
	public String text(Integer v) {return(CFG.ALT_COMBAT_UI.get()?v.toString():"IP: " + v);}
	public Integer value() {return(fv.current.oip);}
    };

    private static Coord actc(int i) {
	int rl = 5;
	return(new Coord((actpitch * (i % rl)) - (((rl - 1) * actpitch) / 2), 125 + ((i / rl) * actpitch)));
    }

    private static final Coord cmc = new Coord(0, 67);
    private static final Coord usec1 = new Coord(-65, 67);
    private static final Coord usec2 = new Coord(65, 67);
    private Indir<Resource> lastact1 = null, lastact2 = null;
    private Text lastacttip1 = null, lastacttip2 = null;
    public void draw(GOut g) {
	updatepos();
        boolean altui = CFG.ALT_COMBAT_UI.get();
	int x0 = ui.gui.calendar.rootpos().x + ui.gui.calendar.sz.x / 2;
	int y0 = ui.gui.calendar.rootpos().y + ui.gui.calendar.sz.y / 2;
	int bottom = ui.gui.beltwdg.c.y - 40;
	double now = Utils.rtime();

	for(Buff buff : fv.buffs.children(Buff.class))
	    buff.draw(g.reclip(altui ? new Coord(x0 - buff.c.x - Buff.cframe.sz().x - 80, y0) : pcc.add(-buff.c.x - Buff.cframe.sz().x - 20, buff.c.y + pho - Buff.cframe.sz().y), buff.sz));
	if(fv.current != null) {
	    for(Buff buff : fv.current.buffs.children(Buff.class))
		buff.draw(g.reclip(altui ? new Coord(x0 + buff.c.x + 80, y0) : pcc.add(buff.c.x + 20, buff.c.y + pho - Buff.cframe.sz().y), buff.sz));

	    g.aimage(ip.get().tex(), altui ? new Coord(x0 - 45, y0-16) : pcc.add(-75, 0), 1, 0.5);
	    g.aimage(oip.get().tex(), altui ? new Coord(x0 + 45, y0-16) : pcc.add(75, 0), 0, 0.5);

	    if(fv.lsrel.size() > 1)
		fxon(fv.current.gobid, tgtfx);
	}

	{
	    Coord cdc = altui ? new Coord(x0, y0) : pcc.add(cmc);
	    if(now < fv.atkct) {
		double a = (now - fv.atkcs) / (fv.atkct - fv.atkcs);
		g.chcolor(255, 0, 128, 224);
		g.fellipse(cdc, altui ? new Coord(24, 24) : new Coord(22, 22), Math.PI / 2 - (Math.PI * 2 * Math.min(1.0 - a, 1.0)), Math.PI / 2);
		g.chcolor();
	    }
	    g.image(cdframe, altui ? new Coord(x0, y0).sub(cdframe.sz().div(2)) : cdc.sub(cdframe.sz().div(2)));
	}
	try {
	    Indir<Resource> lastact = fv.lastact;
	    if(lastact != this.lastact1) {
		this.lastact1 = lastact;
		this.lastacttip1 = null;
	    }
	    double lastuse = fv.lastuse;
	    if(lastact != null) {
		Tex ut = lastact.get().layer(Resource.imgc).tex();
		Coord useul = altui ? new Coord(x0 - 69, y0) : pcc.add(usec1).sub(ut.sz().div(2));
		g.image(ut, useul);
		g.image(useframe, useul.sub(useframeo));
		double a = now - lastuse;
		if(a < 1) {
		    Coord off = new Coord((int)(a * ut.sz().x / 2), (int)(a * ut.sz().y / 2));
		    g.chcolor(255, 255, 255, (int)(255 * (1 - a)));
		    g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
		    g.chcolor();
		}
	    }
	} catch(Loading l) {
	}
	if(fv.current != null) {
	    try {
		Indir<Resource> lastact = fv.current.lastact;
		if(lastact != this.lastact2) {
		    this.lastact2 = lastact;
		    this.lastacttip2 = null;
		}
		double lastuse = fv.current.lastuse;
		if(lastact != null) {
		    Tex ut = lastact.get().layer(Resource.imgc).tex();
		    Coord useul = altui ? new Coord(x0 + 69 - ut.sz().x, y0) : pcc.add(usec2).sub(ut.sz().div(2));
		    g.image(ut, useul);
		    g.image(useframe, useul.sub(useframeo));
		    double a = now - lastuse;
		    if(a < 1) {
			Coord off = new Coord((int)(a * ut.sz().x / 2), (int)(a * ut.sz().y / 2));
			g.chcolor(255, 255, 255, (int)(255 * (1 - a)));
			g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
			g.chcolor();
		    }
		}
	    } catch(Loading l) {
	    }
	}
	for(int i = 0; i < actions.length; i++) {
	    Coord ca = altui ? new Coord(x0 - 18, bottom - 150).add(actc(i)) : pcc.add(actc(i));
	    Action act = actions[i];
	    try {
		if(act != null) {
		    Tex img = act.res.get().layer(Resource.imgc).tex();
		    Coord hsz = img.sz().div(2);
		    g.image(img, ca);
		    if(now < act.ct) {
			double a = (now - act.cs) / (act.ct - act.cs);
			g.chcolor(0, 0, 0, 132);
			g.prect(ca.add(hsz), hsz.inv(), hsz, (1.0 - a) * Math.PI * 2);
			g.chcolor();
			g.aimage(Text.renderstroked(String.format("%.1f", act.ct - now)).tex(), ca.add(hsz.x, 0), 0.5, 0);
		    }
		    if(CFG.SHOW_COMBAT_KEYS.get()) {g.aimage(keytex(i), ca.add(img.sz()), 1, 1);}
		    
		    if(i == use) {
			g.image(indframe, ca.sub(indframeo));
		    } else if(i == useb) {
			g.image(indbframe, ca.sub(indbframeo));
		    } else {
			g.image(actframe, ca.sub(actframeo));
		    }
		}
	    } catch(Loading l) {}
	}
    }
    
    private Tex keytex(int i) {
	if(keytex[i] == null) {
	    keytex[i] = Text.renderstroked(keybinds[i].shortcut(true), fnd).tex();
	}
	return keytex[i];
    }
    
    private Widget prevtt = null;
    private Text acttip = null;
    public static final Tex[] keytex = new Tex[keybinds.length];
    public Object tooltip(Coord c, Widget prev) {
	boolean altui = CFG.ALT_COMBAT_UI.get();
	int x0 =  ui.gui.calendar.rootpos().x + ui.gui.calendar.sz.x / 2;
	int y0 =  ui.gui.calendar.rootpos().y + ui.gui.calendar.sz.y / 2;
	int bottom = ui.gui.beltwdg.c.y - 40;
	for(Buff buff : fv.buffs.children(Buff.class)) {
	    Coord dc = altui ? new Coord(x0 - buff.c.x - Buff.cframe.sz().x - 80, y0) : pcc.add(-buff.c.x - Buff.cframe.sz().x - 20, buff.c.y + pho - Buff.cframe.sz().y);
	    if(c.isect(dc, buff.sz)) {
		Object ret = buff.tooltip(c.sub(dc), prevtt);
		if(ret != null) {
		    prevtt = buff;
		    return(ret);
		}
	    }
	}
	if(fv.current != null) {
	    for(Buff buff : fv.current.buffs.children(Buff.class)) {
		Coord dc = altui ? new Coord(x0 + buff.c.x + 80, y0) : pcc.add(buff.c.x + 20, buff.c.y + pho - Buff.cframe.sz().y);
		if(c.isect(dc, buff.sz)) {
		    Object ret = buff.tooltip(c.sub(dc), prevtt);
		    if(ret != null) {
			prevtt = buff;
			return(ret);
		    }
		}
	    }
	}
	final int rl = 5;
	for(int i = 0; i < actions.length; i++) {
	    Coord ca = altui ? new Coord(x0 - 18, bottom - 150).add(actc(i)).add(16, 16) : pcc.add(actc(i));
	    Indir<Resource> act = (actions[i] == null) ? null : actions[i].res;
	    try {
		if(act != null) {
		    Tex img = act.get().layer(Resource.imgc).tex();
		    ca = ca.sub(img.sz().div(2));
		    if(c.isect(ca, img.sz())) {
			String tip = act.get().layer(Resource.tooltip).t + " ($b{$col[255,128,0]{" + keybinds[i].shortcut(true) + "}})";
			if((acttip == null) || !acttip.text.equals(tip))
			    acttip = RichText.render(tip, -1);
			return(acttip);
		    }
		}
	    } catch(Loading l) {}
	}
	try {
	    Indir<Resource> lastact = this.lastact1;
	    if(lastact != null) {
		Coord usesz = lastact.get().layer(Resource.imgc).sz;
		Coord lac = altui ? new Coord(x0 - 69, y0).add(usesz.div(2)) : pcc.add(usec1);
		if(c.isect(lac.sub(usesz.div(2)), usesz)) {
		    if(lastacttip1 == null)
			lastacttip1 = Text.render(lastact.get().layer(Resource.tooltip).t);
		    return(lastacttip1);
		}
	    }
	} catch(Loading l) {}
	try {
	    Indir<Resource> lastact = this.lastact2;
	    if(lastact != null) {
		Coord usesz = lastact.get().layer(Resource.imgc).sz;
		Coord lac = altui ? new Coord(x0 + 69 - usesz.x, y0).add(usesz.div(2)) : pcc.add(usec2);
		if(c.isect(lac.sub(usesz.div(2)), usesz)) {
		    if(lastacttip2 == null)
			lastacttip2 = Text.render(lastact.get().layer(Resource.tooltip).t);
		    return(lastacttip2);
		}
	    }
	} catch(Loading l) {}
	return(null);
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "act") {
	    int n = (Integer)args[0];
	    if(args.length > 1) {
		Indir<Resource> res = ui.sess.getres((Integer)args[1]);
		actions[n] = new Action(res);
	    } else {
		actions[n] = null;
	    }
	} else if(msg == "acool") {
	    int n = (Integer)args[0];
	    double now = Utils.rtime();
	    actions[n].cs = now;
	    actions[n].ct = now + (((Number)args[1]).doubleValue() * 0.06);
	} else if(msg == "use") {
	    this.use = (Integer)args[0];
	    this.useb = (args.length > 1) ? ((Integer)args[1]) : -1;
	} else if(msg == "used") {
	} else {
	    super.uimsg(msg, args);
	}
    }

    public boolean globtype(char key, KeyEvent ev) {
	if((key == 0) && (ev.getModifiersEx() & (InputEvent.CTRL_DOWN_MASK | KeyEvent.META_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) == 0) {
	    int fn = getAction(ev);
	    if((fn >= 0) && (fn < actions.length)) {
		MapView map = getparent(GameUI.class).map;
		Coord mvc = map.rootxlate(ui.mc);
		if(mvc.isect(Coord.z, map.sz)) {
		    map.delay(map.new Maptest(mvc) {
			    protected void hit(Coord pc, Coord2d mc) {
				wdgmsg("use", fn, 1, ui.modflags(), mc.floor(OCache.posres));
			    }

			    protected void nohit(Coord pc) {
				wdgmsg("use", fn, 1, ui.modflags());
			    }
			});
		}
		return(true);
	    }
	} else if((key == 9) && ((ev.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0)) {
	    Fightview.Relation cur = fv.current;
	    if(cur != null) {
		fv.lsrel.remove(cur);
		fv.lsrel.addLast(cur);
	    }
	    fv.wdgmsg("bump", (int)fv.lsrel.get(0).gobid);
	    return(true);
	}
	return(super.globtype(key, ev));
    }
    
    private int getAction(KeyEvent ev) {
	for (int i = 0; i < actions.length && i < keybinds.length; i++) {
	    if(keybinds[i].match(ev)) {
		return i;
	    }
	}
	return -1;
    }
    
    public static void updateKeybinds(KeyBind[] combat) {
	if(combat != null) {
	    for (int i = 0; i < combat.length && i < keybinds.length; i++) {
		keybinds[i] = combat[i];
	    }
	}
    }
}
