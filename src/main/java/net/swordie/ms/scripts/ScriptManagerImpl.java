package net.swordie.ms.scripts;

import net.swordie.ms.ServerConfig;
import net.swordie.ms.ServerConstants;
import net.swordie.ms.client.Account;
import net.swordie.ms.client.Client;
import net.swordie.ms.client.alliance.Alliance;
import net.swordie.ms.client.alliance.AllianceResult;
import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.MonsterPark;
import net.swordie.ms.client.character.avatar.AvatarLook;
import net.swordie.ms.client.character.damage.DamageSkinSaveData;
import net.swordie.ms.client.character.damage.DamageSkinType;
import net.swordie.ms.client.character.items.Inventory;
import net.swordie.ms.client.character.items.Item;
import net.swordie.ms.client.character.items.ItemBuffs;
import net.swordie.ms.client.character.quest.Quest;
import net.swordie.ms.client.character.quest.QuestManager;
import net.swordie.ms.client.character.scene.Scene;
import net.swordie.ms.client.character.skills.Option;
import net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat;
import net.swordie.ms.client.character.skills.temp.TemporaryStatBase;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.client.guild.Guild;
import net.swordie.ms.client.guild.GuildMember;
import net.swordie.ms.client.guild.result.GuildResult;
import net.swordie.ms.client.guild.result.GuildType;
import net.swordie.ms.client.party.Party;
import net.swordie.ms.client.party.PartyMember;
import net.swordie.ms.client.trunk.TrunkOpen;
import net.swordie.ms.connection.db.DatabaseManager;
import net.swordie.ms.connection.packet.*;
import net.swordie.ms.constants.*;
import net.swordie.ms.enums.*;
import net.swordie.ms.handlers.EventManager;
import net.swordie.ms.life.DeathType;
import net.swordie.ms.life.Life;
import net.swordie.ms.life.Reactor;
import net.swordie.ms.life.drop.Drop;
import net.swordie.ms.life.mob.Mob;
import net.swordie.ms.life.mob.skill.MobSkill;
import net.swordie.ms.life.mob.skill.MobSkillID;
import net.swordie.ms.life.npc.Npc;
import net.swordie.ms.life.npc.NpcMessageType;
import net.swordie.ms.life.npc.NpcScriptInfo;
import net.swordie.ms.loaders.*;
import net.swordie.ms.loaders.containerclasses.ItemInfo;
import net.swordie.ms.loaders.containerclasses.MobSkillInfo;
import net.swordie.ms.util.FileTime;
import net.swordie.ms.util.Position;
import net.swordie.ms.util.Rect;
import net.swordie.ms.util.Util;
import net.swordie.ms.util.container.Tuple;
import net.swordie.ms.world.World;
import net.swordie.ms.world.event.*;
import net.swordie.ms.world.field.*;
import net.swordie.ms.world.field.fieldeffect.FieldEffect;
import net.swordie.ms.world.field.fieldeffect.GreyFieldType;
import net.swordie.ms.world.field.obtacleatom.ObtacleAtomInfo;
import net.swordie.ms.world.field.obtacleatom.ObtacleInRowInfo;
import net.swordie.ms.world.field.obtacleatom.ObtacleRadianInfo;
import net.swordie.ms.world.shop.NpcShopDlg;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.script.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static net.swordie.ms.client.character.skills.temp.CharacterTemporaryStat.RideVehicle;
import static net.swordie.ms.enums.ChatType.*;
import static net.swordie.ms.life.mob.skill.MobSkillStat.fixDamR;
import static net.swordie.ms.life.npc.NpcMessageType.*;

/**
 * Created on 2/19/2018.
 *
 * @see ScriptManager
 */
public class ScriptManagerImpl implements ScriptManager {
	public static final String SCRIPT_ENGINE_NAME = "python";
	public static final String QUEST_START_SCRIPT_END_TAG = "s";
	public static final String QUEST_COMPLETE_SCRIPT_END_TAG = "e";

	private static final ExecutorService SCRIPT_EXECUTOR_SERVICE = Executors.newCachedThreadPool();
	private static final String SCRIPT_ENGINE_EXTENSION = ".py";
	private static final String DEFAULT_SCRIPT = "undefined";
	private static final String INTENDED_NPE_MSG = "Intended NPE by forceful script stop.";
	private static final Logger log = LogManager.getRootLogger();
	private static final ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName(SCRIPT_ENGINE_NAME);

	private Char chr;
	private Field field;
	private final boolean isField;
	private NpcScriptInfo npcScriptInfo;
	private Map<ScriptType, ScriptInfo> scripts;
	private int returnField = 0;
	private ScriptType lastActiveScriptType;
	private Map<ScriptType, Future> evaluations = new HashMap<>();
	private Set<ScheduledFuture> events = new HashSet<>();
	private ScriptMemory memory = new ScriptMemory();
	private boolean curNodeEventEnd;
	private static final Lock fileReadLock = new ReentrantLock();
	private FieldTransferInfo fieldTransferInfo;
	private int objectID;

	private ScriptManagerImpl(Char chr, Field field) {
		this.chr = chr;
		this.field = field;
		this.npcScriptInfo = new NpcScriptInfo();
		this.scripts = new HashMap<>();
		this.isField = chr == null;
		this.lastActiveScriptType = ScriptType.None;
		this.fieldTransferInfo = new FieldTransferInfo();
	}

	public ScriptManagerImpl(Char chr) {
		this(chr, chr.getField());
	}

	public ScriptManagerImpl(Field field) {
		this(null, field);
	}

	private Bindings getBindingsByType(ScriptType scriptType) {
		ScriptInfo si = getScriptInfoByType(scriptType);
		return si == null ? null : si.getBindings();
	}

	public ScriptInfo getScriptInfoByType(ScriptType scriptType) {
		return scripts.getOrDefault(scriptType, null);
	}

	@Override
	public Char getChr() {
		return chr;
	}

	public String getScriptNameByType(ScriptType scriptType) {
		return getScriptInfoByType(scriptType).getScriptName();
	}

	public Invocable getInvocableByType(ScriptType scriptType) {
		return getScriptInfoByType(scriptType).getInvocable();
	}

	public int getParentIDByScriptType(ScriptType scriptType) {
		return getScriptInfoByType(scriptType) != null ? getScriptInfoByType(scriptType).getParentID() : 2007;
	}

	public int getObjectIDByScriptType(ScriptType scriptType) {
		return getScriptInfoByType(scriptType) != null ? getScriptInfoByType(scriptType).getObjectID() : 0;
	}

	public void startScript(int parentID, ScriptType scriptType) {
		startScript(parentID, 0, scriptType);
	}

	public void startScript(int parentID, String scriptName, ScriptType scriptType) {
		startScript(parentID, 0, scriptName, scriptType);
	}

	public void startScript(int parentID, int objID, ScriptType scriptType) {
		startScript(parentID, objID, parentID + ".py", scriptType);
	}

	public void startScript(int parentID, int objID, String scriptName, ScriptType scriptType) {
		if (scriptType == ScriptType.None || (scriptType == ScriptType.Quest && !isQuestScriptAllowed())) {
			log.debug(String.format("Did not allow script %s to go through (type %s)  |  Active Script Type: %s", scriptName, scriptType, getLastActiveScriptType()));
			return;
		}
		setLastActiveScriptType(scriptType);
		if (isActive(scriptType) && (scriptType != ScriptType.Field && scriptType != ScriptType.FirstEnterField)) { // because Field Scripts don't get disposed.
			chr.chatMessage(String.format("Already running a script of the same type (%s, id %d)! Type @check if this" +
							" is not intended.", scriptType.getDir(), getScriptInfoByType(scriptType).getParentID()));
			log.debug(String.format("Could not run script %s because one of the same type is already running (%s, type %s)",
					scriptName, getScriptInfoByType(scriptType).getScriptName(), scriptType));
			return;
		}
		if (!isField()) {
			chr.dbgChatMsg(String.format("Starting script %s, scriptType %s.", scriptName, scriptType));
			log.debug(String.format("Starting script %s, scriptType %s.", scriptName, scriptType));
		}
		objectID = objID;
		resetParam();
		Bindings bindings = getBindingsByType(scriptType);
		if (bindings == null) {
			bindings = scriptEngine.createBindings();
			bindings.put("sm", this);
			bindings.put("chr", chr);
		}
		bindings.put("field", chr == null ? field : chr.getField());
		bindings.put("parentID", parentID);
		bindings.put("scriptType", scriptType);
		bindings.put("objectID", objID);
		if (scriptType == ScriptType.Reactor) {
			bindings.put("reactor", chr.getField().getLifeByObjectID(objID));
		}
		if (scriptType == ScriptType.Quest) {
			bindings.put("startQuest",
					scriptName.charAt(scriptName.length() - 1) == QUEST_START_SCRIPT_END_TAG.charAt(0)); // biggest hack eu
		}
		ScriptInfo scriptInfo = new ScriptInfo(scriptType, bindings, parentID, scriptName);
		if (scriptType == ScriptType.Npc) {
			getNpcScriptInfo().setTemplateID(parentID);
		}
		scriptInfo.setObjectID(objID);
		getScripts().put(scriptType, scriptInfo);
		SCRIPT_EXECUTOR_SERVICE.execute(() -> startScript(scriptName, scriptType)); // makes the script execute async
	}

	private boolean isQuestScriptAllowed() {
		return getLastActiveScriptType() == ScriptType.None;
	}

	private void notifyMobDeath(Mob mob) {
		if (isActive(ScriptType.FirstEnterField)) {
			getScriptInfoByType(ScriptType.FirstEnterField).addResponse(mob);
		} else if (isActive(ScriptType.Field)) {
			getScriptInfoByType(ScriptType.Field).addResponse(mob);
		}
	}

	private void startScript(String name, ScriptType scriptType) {
		String dir = String.format("%s/%s/%s%s", ServerConstants.SCRIPT_DIR,
				scriptType.getDir().toLowerCase(), name, SCRIPT_ENGINE_EXTENSION);
		boolean exists = new File(dir).exists();
		if (!exists) {
			log.error(String.format("[Error] Could not find script %s/%s", scriptType.getDir().toLowerCase(), name));
			if(chr != null) {
				chr.dbgChatMsg(String.format("[Script] Could not find script %s/%s", scriptType.getDir().toLowerCase(), name));
			}

			if (ServerConfig.AUTO_CREATE_UNCODED_SCRIPTS) {
				dir = String.format("%s/%s/%s%s", ServerConstants.SCRIPT_DIR, // dev will remove script prefix when script has been coded
						scriptType.getDir().toLowerCase(), "autogen_" + name, SCRIPT_ENGINE_EXTENSION);
				try {
					ScriptInfo info = getScriptInfoByType(scriptType);
					List<String> content = new ArrayList<>(Util.makeSet(
							"# Character field ID when accessed: " + getFieldID(),
							"# ObjectID: " + info.getObjectID(),
							"# ParentID: " + info.getParentID()
					));
					switch (scriptType) {
						case Portal:
						case Reactor:
						case Npc:
							content.addAll(Util.makeSet(
									"# Object Position X: " + getObjectPositionX(),
									"# Object Position Y: " + getObjectPositionY()
							));
					}

					Util.createAndWriteToFile(dir, content);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			dir = String.format("%s/%s/%s%s", ServerConstants.SCRIPT_DIR,
					scriptType.getDir().toLowerCase(), DEFAULT_SCRIPT, SCRIPT_ENGINE_EXTENSION);
		}
		ScriptInfo si = getScriptInfoByType(scriptType);
		si.setActive(true);
		CompiledScript cs;
		getScriptInfoByType(scriptType).setFileDir(dir);
		StringBuilder script = new StringBuilder();
		ScriptEngine se = scriptEngine;
		Bindings bindings = getBindingsByType(scriptType);
		si.setInvocable((Invocable) se);
		try {
			fileReadLock.lock();
			script.append(Util.readFile(dir, Charset.defaultCharset()));
		} catch (IOException e) {
			e.printStackTrace();
			lockInGameUI(false); // so players don't get stuck if a script fails
		} finally {
			fileReadLock.unlock();
		}
		try {
			cs = ((Compilable) se).compile(script.toString());
			cs.eval(bindings);
		} catch (ScriptException e) {
			if (!e.getMessage().contains(INTENDED_NPE_MSG)) {
				log.error(String.format("Unable to compile script %s!", name));
				e.printStackTrace();
				lockInGameUI(false); // so players don't get stuck if a script fails
			}
		} finally {
			if (si.isActive() && name.equals(si.getScriptName()) &&
					((scriptType != ScriptType.Field && scriptType != ScriptType.FirstEnterField)
							|| (chr != null && chr.getFieldID() == si.getParentID()))) {
				// gracefully stop script if it's still active with the same script info (scriptName, or scriptName +
				// current chr fieldID == fieldscript's fieldID if scriptType == Field).
				// This makes it so field scripts won't cancel new field scripts when having a warp() in them.
				stop(scriptType);
			}
			FieldTransferInfo fti = getFieldTransferInfo();
			if (!fti.isInit()) {
				if (fti.isField()) {
					fti.warp(field);
				} else {
					fti.warp(chr);
				}
			}
		}
	}

	public void stop(ScriptType scriptType) {
		setSpeakerID(0);
		if (getLastActiveScriptType() == scriptType) {
			setLastActiveScriptType(ScriptType.None);
		}
		ScriptInfo si = getScriptInfoByType(scriptType);
		if (si != null) {
			si.reset();
		}
		getMemory().clear();
		if (chr != null) {
			WvsContext.dispose(chr);
		}
	}

	@Override
	public void update(Observable o, Object arg) {
		if (o instanceof Mob) {
			notifyMobDeath((Mob) o);
		}
	}

	public void handleAction(NpcMessageType lastType, byte response, int answer) {
		handleAction(getLastActiveScriptType(), lastType, response, answer, null);
	}

	public void handleAction(NpcMessageType lastType, byte response, String text) {
		handleAction(getLastActiveScriptType(), lastType, response, 0, text);
	}

	public void handleAction(ScriptType scriptType, NpcMessageType lastType, byte response, int answer, String text) {
		switch (response) {
			case -1:
			case 5:
				stop(scriptType);
				break;
            default:
				ScriptMemory sm = getMemory();
				if (lastType.isPrevPossible() && response == 0) {
					// back button pressed
					NpcScriptInfo prev = sm.getPreviousScriptInfo();
					chr.write(ScriptMan.scriptMessage(prev, prev.getMessageType()));
				} else {
					if (getMemory().isInMemory()) {
						NpcScriptInfo next = sm.getNextScriptInfo();
						chr.write(ScriptMan.scriptMessage(next, next.getMessageType()));
					} else {
						ScriptInfo si = getScriptInfoByType(scriptType);
						if (isActive(scriptType)) {
							switch (lastType.getResponseType()) {
								case Response:
									si.addResponse((int) response);
									break;
								case Answer:
									si.addResponse(answer);
									break;
								case Text:
									si.addResponse(text);
									break;
							}
						}
					}
				}
		}
	}

	public boolean isActive(ScriptType scriptType) {
		return getScriptInfoByType(scriptType) != null && getScriptInfoByType(scriptType).isActive();
	}

	public NpcScriptInfo getNpcScriptInfo() {
		return npcScriptInfo;
	}

	public Map<ScriptType, ScriptInfo> getScripts() {
		return scripts;
	}

	public int getParentID() {
		int res = 0;
		for (ScriptType type : ScriptType.values()) {
			if (getScriptInfoByType(type) != null) {
				res = getScriptInfoByType(type).getParentID();
			}
		}
		return res;
	}

	public boolean isField() {
		return isField;
	}

	public Field getField() {
		return field;
	}

	public ScriptType getLastActiveScriptType() {
		return lastActiveScriptType;
	}

	public void setLastActiveScriptType(ScriptType lastActiveScriptType) {
		this.lastActiveScriptType = lastActiveScriptType;
	}

	public FieldTransferInfo getFieldTransferInfo() {
		return fieldTransferInfo;
	}

	public void setFieldTransferInfo(FieldTransferInfo fieldTransferInfo) {
		this.fieldTransferInfo = fieldTransferInfo;
	}

	// Start of the sends/asks -----------------------------------------------------------------------------------------

	@Override
	public int sendSay(String text) {
		if (getLastActiveScriptType() == ScriptType.None) {
			return 0;
		}
		return sendGeneralSay(text, Say);
	}

	/**
	 * Helper function that ensures that selections have the appropriate type (AskMenu).
	 *
	 * @param text
	 * @param nmt
	 */
	private int sendGeneralSay(String text, NpcMessageType nmt) throws NullPointerException {
		getNpcScriptInfo().setText(text);
		if (text.contains("#L")) {
			nmt = AskMenu;
		}
		getNpcScriptInfo().setMessageType(nmt);
		chr.write(ScriptMan.scriptMessage(getNpcScriptInfo(), nmt));
		getMemory().addMemoryInfo(getNpcScriptInfo());
		Object response = null;
		if (isActive(getLastActiveScriptType())) {
			response = getScriptInfoByType(getLastActiveScriptType()).awaitResponse();
		}
		if (response == null) {
			throw new NullPointerException(INTENDED_NPE_MSG);
		}
		return (int) response;
	}

	@Override
	public int sendNext(String text) {
		return sendGeneralSay(text, SayNext);
	}

	@Override
	public int sendPrev(String text) {
		return sendGeneralSay(text, SayPrev);
	}

	@Override
	public int sendSayOkay(String text) {
		return sendGeneralSay(text, SayOk);
	}

	@Override
	public int sendSayImage(String image) {
		return sendSayImage(new String[]{image});
	}

	@Override
	public int sendSayImage(String[] images) {
		getNpcScriptInfo().setImages(images);
		getNpcScriptInfo().setMessageType(SayImage);
		return sendGeneralSay("", SayImage);
	}

	@Override
	public boolean sendAskYesNo(String text) {
		return sendGeneralSay(text, AskYesNo) != 0;
	}

	@Override
	public boolean sendAskAccept(String text) {
		return sendGeneralSay(text, AskAccept) != 0;
	}

	@Override
	public String sendAskText(String text, String defaultText, short minLength, short maxLength) throws NullPointerException {
		getNpcScriptInfo().setMin(minLength);
		getNpcScriptInfo().setMax(maxLength);
		getNpcScriptInfo().setDefaultText(defaultText);
		getNpcScriptInfo().setText(text);
		getNpcScriptInfo().setMessageType(AskText);
		chr.write(ScriptMan.scriptMessage(getNpcScriptInfo(), AskText));
		getMemory().addMemoryInfo(getNpcScriptInfo());
		Object response = getScriptInfoByType(getLastActiveScriptType()).awaitResponse();
		if (response == null) {
			throw new NullPointerException("Intended");
		}
		return (String) response;
	}

	@Override
	public int sendAskNumber(String text, int defaultNum, int min, int max) {
		getNpcScriptInfo().setDefaultNumber(defaultNum);
		getNpcScriptInfo().setMin(min);
		getNpcScriptInfo().setMax(max);
		return sendGeneralSay(text, AskNumber);
	}

	@Override
	public int sendInitialQuiz(byte type, String title, String problem, String hint, int min, int max, int time) {
		NpcScriptInfo nsi = getNpcScriptInfo();
		nsi.setType(type);
		if (type != 1) {
			nsi.setTitle(title);
			nsi.setProblemText(problem);
			nsi.setHintText(hint);
			nsi.setMin(min);
			nsi.setMax(max);
			nsi.setTime(time);
		}
		return sendGeneralSay(title, InitialQuiz);
	}

	@Override
	public int sendInitialSpeedQuiz(byte type, int quizType, int answer, int correctAnswers, int remaining, int time) {
		NpcScriptInfo nsi = getNpcScriptInfo();
		nsi.setType(type);
		if (type != 1) {
			nsi.setQuizType(quizType);
			nsi.setAnswer(answer);
			nsi.setCorrectAnswers(correctAnswers);
			nsi.setRemaining(remaining);
			nsi.setTime(time);
		}
		return sendGeneralSay("", InitialSpeedQuiz);
	}

	@Override
	public int sendICQuiz(byte type, String text, String hintText, int time) {
		getNpcScriptInfo().setType(type);
		getNpcScriptInfo().setHintText(hintText);
		getNpcScriptInfo().setTime(time);
		return sendGeneralSay(text, ICQuiz);
	}

	@Override
	public int sendAskAvatar(String text, boolean angelicBuster, boolean zeroBeta, int... options) {
		getNpcScriptInfo().setAngelicBuster(angelicBuster);
		getNpcScriptInfo().setZeroBeta(zeroBeta);
		getNpcScriptInfo().setOptions(options);
		return sendGeneralSay(text, AskAvatar);
	}

	public int sendAskSlideMenu(int dlgType) {
		getNpcScriptInfo().setDlgType(dlgType);
		return sendGeneralSay("", AskSlideMenu);
	}

	public int sendAskSelectMenu(int dlgType, int defaultSelect) {
		return sendAskSelectMenu(dlgType, defaultSelect, new String[]{});
	}

	public int sendAskSelectMenu(int dlgType, int defaultSelect, String[] text) {
		getNpcScriptInfo().setDlgType(dlgType);
		getNpcScriptInfo().setDefaultSelect(defaultSelect);
		getNpcScriptInfo().setSelectText(text);
		return sendGeneralSay("", AskSelectMenu);
	}


	// Start of param methods ------------------------------------------------------------------------------------------

	public void resetParam() {
		getNpcScriptInfo().resetParam();
	}

	public void removeEscapeButton() {
		getNpcScriptInfo().addParam(NpcScriptInfo.Param.NotCancellable);
	}

	public void addEscapeButton() {
		if(getNpcScriptInfo().hasParam(NpcScriptInfo.Param.NotCancellable)) {
			getNpcScriptInfo().removeParam(NpcScriptInfo.Param.NotCancellable);
		}
	}

	public void flipSpeaker() {
		getNpcScriptInfo().addParam(NpcScriptInfo.Param.FlipSpeaker);
	}

	public void flipDialogue() {
		getNpcScriptInfo().addParam(NpcScriptInfo.Param.OverrideSpeakerID);
	}

	public void flipDialoguePlayerAsSpeaker() {
		getNpcScriptInfo().addParam(NpcScriptInfo.Param.PlayerAsSpeakerFlip);
	}

	public void setPlayerAsSpeaker() {
		getNpcScriptInfo().addParam(NpcScriptInfo.Param.PlayerAsSpeaker);
	}

	public void setBoxChat() {
		setBoxChat(true);
	}

	public void setBoxChat(boolean color) { // true = Standard BoxChat  |  false = Zero BoxChat
		getNpcScriptInfo().setColor((byte) (color ? 1 : 0));
		getNpcScriptInfo().addParam(NpcScriptInfo.Param.BoxChat);
	}

	public void flipBoxChat() {
		getNpcScriptInfo().addParam(NpcScriptInfo.Param.FlipBoxChat);
	}

	public void boxChatPlayerAsSpeaker() {
		getNpcScriptInfo().addParam(NpcScriptInfo.Param.BoxChatAsPlayer);
	}

	public void flipBoxChatPlayerAsSpeaker() {
		getNpcScriptInfo().addParam(NpcScriptInfo.Param.FlipBoxChatAsPlayer);
	}

	public void flipBoxChatPlayerNoEscape() {
		getNpcScriptInfo().addParam(NpcScriptInfo.Param.FlipBoxChatAsPlayerNoEscape);
	}



	// Start helper methods for scripts --------------------------------------------------------------------------------

	@Override
	public void dispose() {
		dispose(true);
	}

	public void dispose(boolean stop) {
		getNpcScriptInfo().reset();
		getMemory().clear();
		stop(ScriptType.Npc);
		stop(ScriptType.Portal);
		stop(ScriptType.Item);
		stop(ScriptType.Quest);
		stop(ScriptType.Reactor);
		if (stop) {
			throw new NullPointerException(INTENDED_NPE_MSG); // makes the underlying script stop
		}
		setCurNodeEventEnd(false);
	}

	public void dispose(ScriptType scriptType) {
		getMemory().clear();
		stop(scriptType);
	}

	public Position getPosition(int objId) {
		return chr.getField().getLifeByObjectID(objId).getPosition();
	}


	// Character Stat-related methods ----------------------------------------------------------------------------------

	@Override
	public void setJob(short jobID) {
		chr.setJob(jobID);
		Map<Stat, Object> stats = new HashMap<>();
		stats.put(Stat.subJob, jobID);
		chr.getClient().write(WvsContext.statChanged(stats));
	}

	public void addSP(int amount) {
		addSP(amount, false);
	}

	@Override
	public void addSP(int amount, boolean jobAdv) {
		byte jobLevel = (byte) JobConstants.getJobLevel(chr.getJob());
		int currentSP = chr.getAvatarData().getCharacterStat().getExtendSP().getSpByJobLevel(jobLevel);
		setSP(currentSP + amount);
		if (jobAdv) {
			chr.write(WvsContext.incSpMessage(chr.getJob(), (byte) amount));
		}
	}

	@Override
	public void setSP(int amount) {
		chr.setSpToCurrentJob(amount);
		Map<Stat, Object> stats = new HashMap<>();
		stats.put(Stat.sp, chr.getAvatarData().getCharacterStat().getExtendSP());
		chr.getClient().write(WvsContext.statChanged(stats));
	}

	@Override
	public void addAP(int amount) {
		int currentAP = chr.getAvatarData().getCharacterStat().getAp();
		setAP(currentAP + amount);
	}

	@Override
	public void setAP(int amount) {
		chr.setStat(Stat.ap, (short) amount);
		Map<Stat, Object> stats = new HashMap<>();
		stats.put(Stat.ap, (short) amount);
		chr.getClient().write(WvsContext.statChanged(stats));
	}

	@Override
	public void resetAP(boolean hpmp) {
	    resetAP(hpmp, (short) 0);
	}

	@Override
    public void resetAP(boolean hpmp, short jobID) {
        Map<Stat, Object> stats = new HashMap<>();
        stats.put(Stat.str,  chr.getAvatarData().getCharacterStat().getStr());
        stats.put(Stat.dex,  chr.getAvatarData().getCharacterStat().getDex());
        stats.put(Stat.luk,  chr.getAvatarData().getCharacterStat().getLuk());
        stats.put(Stat.inte, chr.getAvatarData().getCharacterStat().getInt());
        stats.put(Stat.ap,   chr.getAvatarData().getCharacterStat().getAp());

        // Identify Primary Stat, special case only exist for Thief (Xenon) & Pirates (Also including Xenon)
        Stat primaryStat = JobConstants.isWarriorEquipJob(jobID) ? Stat.str : JobConstants.isArcherEquipJob(jobID) ? Stat.dex : JobConstants.isMageEquipJob(jobID) ? Stat.inte : JobConstants.isThiefEquipJob(jobID) ? Stat.luk : Stat.ap;
        if (JobConstants.isXenon(jobID) || JobConstants.isAdventurerPirate(jobID) && !JobConstants.isBuccaneer(jobID) && !JobConstants.isCorsair(jobID) && !JobConstants.isCannonShooter(jobID)) {
            primaryStat = Stat.ap; // If we are a xenon or a 1st job adventurer pirate put points back into AP
        }
        else if (JobConstants.isBuccaneer(jobID) || JobConstants.isThunderBreaker(jobID) || JobConstants.isShade(jobID) || JobConstants.isCannonShooter(jobID)) {
            primaryStat = Stat.str; // Handle STR pirates
        }
        else if (JobConstants.isPirateEquipJob(jobID)) {
            primaryStat = Stat.dex; // if none of the above conditions apply & we're a pirate, only remaining choice is DEX, otherwise we leave primary stat as AP
        }

        int buffer = 0; // Difference between the stat's current value and its minimum (0 for AP, 4 for the four traditional stats)
        for (Map.Entry<Stat, Object> stat : stats.entrySet()) {
            if (stat.getKey() != primaryStat) {
                buffer = ((short) stat.getValue() - (stat.getKey() != Stat.ap ? 4 : 0));
                if (buffer > 0) {
                    stat.setValue((short)((short) stat.getValue() - buffer));
                    stats.put(primaryStat, (short)((short) stats.get(primaryStat) + buffer));
                    chr.setStat(stat.getKey(), (short)stats.get(stat.getKey()));
                    chr.setStat(primaryStat, (short)stats.get(primaryStat));
                }
            }
        }
        chr.getClient().write(WvsContext.statChanged(stats));
    }

	@Override
	public void setSTR(short amount) {
		chr.setStat(Stat.str, amount);
		Map<Stat, Object> stats = new HashMap<>();
		stats.put(Stat.str, amount);
		chr.getClient().write(WvsContext.statChanged(stats));
	}

	@Override
	public void setINT(short amount) {
		chr.setStat(Stat.inte, amount);
		Map<Stat, Object> stats = new HashMap<>();
		stats.put(Stat.inte, amount);
		chr.getClient().write(WvsContext.statChanged(stats));
	}

	@Override
	public void setDEX(short amount) {
		chr.setStat(Stat.dex, amount);
		Map<Stat, Object> stats = new HashMap<>();
		stats.put(Stat.dex, amount);
		chr.getClient().write(WvsContext.statChanged(stats));
	}

	@Override
	public void setLUK(short amount) {
		chr.setStat(Stat.luk, amount);
		Map<Stat, Object> stats = new HashMap<>();
		stats.put(Stat.luk, amount);
		chr.getClient().write(WvsContext.statChanged(stats));
	}

	public void addMaxHP(int amount) {
		chr.addStatAndSendPacket(Stat.mhp, amount);
	}

	@Override
	public void setMaxHP(int amount) {
		chr.setStat(Stat.mhp, amount);
		chr.setStat(Stat.hp, amount);
		Map<Stat, Object> stats = new HashMap<>();
		stats.put(Stat.mhp, amount);
		stats.put(Stat.hp, amount);
		chr.getClient().write(WvsContext.statChanged(stats));
	}

	public void addMaxMP(int amount) {
		chr.addStatAndSendPacket(Stat.mmp, amount);
	}

	@Override
	public void setMaxMP(int amount) {
		chr.setStat(Stat.mmp, amount);
		chr.setStat(Stat.mp, amount);
		Map<Stat, Object> stats = new HashMap<>();
		stats.put(Stat.mmp, amount);
		stats.put(Stat.mp, amount);
		chr.getClient().write(WvsContext.statChanged(stats));
	}

	@Override
	public void jobAdvance(short jobID) {
		setJob(jobID);

		int apAmount = 0;
		int spAmount = 0;

		switch (JobConstants.getJobLevel(jobID)) {
			case 1:
				spAmount = 5;
				if (JobConstants.isCygnusKnight(jobID)) {
                    --spAmount;
                }
				break;
			case 2:
				spAmount = 4;
				if (JobConstants.isDualBlade(jobID)) {
                    ++spAmount;
                }
				break;
			case 3:
				spAmount = 4;
				if (!JobConstants.isDualBlade(jobID)) {
                    apAmount = 5;
                }
				else {
                    spAmount = 10;
                }
				break;
			case 4:
				spAmount = 3;
				if (!JobConstants.isDualBlade(jobID)) {
                    apAmount = 5;
                }
				else {
                    spAmount = 10;
                }
				break;
			case 5:
				apAmount = 5;
				spAmount = 0;
				break;
			case 6:
				apAmount = 5;
				spAmount = 3;
				break;
			}

		addAP(apAmount); //Standard added AP upon Job Advancing
		addSP(spAmount); //Standard added SP upon Job Advancing
	}

	@Override
	public void giveExp(long expGiven) {
		chr.addExp(expGiven);
	}

	@Override
	public void giveExpNoMsg(long expGiven) {
		chr.addExpNoMsg(expGiven);
	}

	@Override
	public void changeCharacterLook(int look) {
		AvatarLook al = chr.getAvatarData().getAvatarLook();
		if (look < 100) { // skin
			al.setSkin(look);
			chr.setStatAndSendPacket(Stat.skin, look);
		} else if (look < 30000) {
			al.setFace(look);
			chr.setStatAndSendPacket(Stat.face, look);
		} else if (look < 1000000) {
			al.setHair(look);
			chr.setStatAndSendPacket(Stat.hair, look);
		} else {
			log.error(String.format("Tried changing a look with invalid id (%d)", look));
		}
	}

	public void giveSkill(int skillId) {
		giveSkill(skillId, 1);
	}

	public void giveSkill(int skillId, int slv) { giveSkill(skillId, slv, slv); }

	@Override
	public void giveSkill(int skillId, int slv, int maxLvl) { chr.addSkill(skillId, slv, maxLvl); }

	public void removeSkill(int skillId) {
		chr.removeSkillAndSendPacket(skillId);
	}

	public int getSkillByItem() {
		return getSkillByItem(getParentID());
	}

	public int getSkillByItem(int itemId) {
		ItemInfo itemInfo = ItemData.getItemInfoByID(itemId);
		return itemInfo.getSkillId();
	}

	public boolean hasSkill(int skillId) {
		return chr.hasSkill(skillId);
	}

	public void heal() {
		chr.heal(chr.getMaxHP());
		chr.healMP(chr.getMaxMP());
	}

	public void addLevel(int level) {
		int curLevel = chr.getLevel();
		for (int i = curLevel + 1; i <= curLevel + level; i++) {
			chr.setStat(Stat.level, i);
			Map<Stat, Object> stats = new HashMap<>();
			stats.put(Stat.level, (byte) i);
			stats.put(Stat.exp, (long) 0);
			chr.getClient().write(WvsContext.statChanged(stats));
			chr.getJobHandler().handleLevelUp();
			chr.getField().broadcastPacket(UserRemote.effect(chr.getId(), Effect.levelUpEffect()));
		}
	}

	public void lockInGameUI(boolean lock) {
		lockInGameUI(lock, true);
	}

	public void lockInGameUI(boolean lock, boolean blackFrame) {
		if (chr != null) {
			chr.write(UserLocal.setInGameDirectionMode(lock, blackFrame, false));
		}
	}

	public void curNodeEventEnd(boolean enable) {
		setCurNodeEventEnd(enable);
		chr.write(FieldPacket.curNodeEventEnd(enable));
	}

	public void setCurNodeEventEnd(boolean curNodeEventEnd) {
		this.curNodeEventEnd = curNodeEventEnd;
	}

	public void progressMessageFont(int fontNameType, int fontSize, int fontColorType, int fadeOutDelay, String message) {
		chr.write(UserPacket.progressMessageFont(fontNameType, fontSize, fontColorType, fadeOutDelay, message));
	}

	public void localEmotion(int emotion, int duration, boolean byItemOption) {
		chr.write(UserLocal.emotion(emotion, duration, byItemOption));
	}



	// Field-related methods -------------------------------------------------------------------------------------------

	@Override
	public void warp(int id) {
		warp(id, 0);
	}

	public void warp(int id, boolean executeAfterScript) {
		warp(id, 0, executeAfterScript, false);
	}

	@Override
	public void warp(int id, int pid) {
		warp(id, pid, true, false);
	}

	public void warp(int id, int pid, boolean instanceField) {
		warp(id, pid, true, instanceField);
	}

	public void warp(int mid, int pid, boolean executeAfterScript, boolean instanceField) {
		if (executeAfterScript) {
			FieldTransferInfo fti = getFieldTransferInfo();
			fti.setFieldId(mid);
			fti.setPortal(pid);
			fti.setIsInstanceField(instanceField);
		} else {
			chr.warp(mid, pid);
		}
	}

	public void changeChannelAndWarp(int channel, int fieldID, boolean executeAfterScript, boolean instanceField) {
		if (executeAfterScript) {
			FieldTransferInfo fti = getFieldTransferInfo();
			fti.setChannel(channel);
			fti.setFieldId(fieldID);
			fti.setIsInstanceField(instanceField);
		} else {
			Client c = chr.getClient();
			c.setOldChannel(c.getChannel());
			chr.changeChannelAndWarp((byte) channel, fieldID);
		}
	}

	public void warpField(int fieldId) {
		warp(fieldId, 0);
	}

	public void warpField(int fieldId, int portalId) {
		// only warp after script has ended
		FieldTransferInfo fti = getFieldTransferInfo();
		fti.setFieldId(fieldId);
		fti.setPortal(portalId);
	}

	public void changeChannelAndWarp(int channel, int fieldID) {
		changeChannelAndWarp(channel, fieldID, true, false);
	}

	@Override
	public int getFieldID() {
		return chr.getField().getId();
	}

	public void warpInstanceOut() {
		warpInstance(-1, false, 0, false);
	}

	public void warpInstanceIn(int id, int portal) {
		warpInstance(id, true, portal, false);
	}

	public void warpInstanceIn(int id, int portalId, boolean partyAllowed) {
		warpInstance(id, true, portalId, partyAllowed);
	}

	public void warpInstanceOut(int id, int portal) {
		warpInstance(id, false, portal, false);
	}

	@Override
	public void warpInstanceIn(int id) {
		warpInstance(id, true, 0, false);
	}

	public void warpInstanceIn(int id, boolean partyAllowed) {
		warpInstance(id, true, 0, partyAllowed);
	}

	@Override
	public void warpInstanceOut(int id) {
		warpInstance(id, false, 0, false);
	}

	private void warpInstance(int fieldId, boolean in, int portalId, boolean partyAllowed) {
		Instance instance;
		if (in) {
			// warp party in if there is a party and party is allowed, solo instance otherwise
			Party party = chr.getParty();
			if (party == null || !partyAllowed) {
				instance = new Instance(chr);
			} else {
				instance = new Instance(party);
			}
			// setup the instance & warp
			instance.setup(fieldId, portalId);
		} else {
			instance = chr.getInstance();
			stopEvents();
			if (instance == null) {
				// no info, just warp them
				chr.setDeathCount(-1);
				chr.warp(fieldId, portalId, false);
			} else {
				// remove chr from eligible instance members
				int forcedReturn;
				int forcedReturnPortal;
				if (fieldId >= 0) {
					forcedReturn = fieldId;
					forcedReturnPortal = -1;
				} else {
					forcedReturn = instance.getForcedReturn();
					forcedReturnPortal = instance.getForcedReturnPortalId();
				}
				instance.removeChar(chr);
				chr.setDeathCount(-1);
				if (forcedReturnPortal >= 0) {
					chr.warp(forcedReturn, forcedReturnPortal, false);
				} else {
					chr.warp(forcedReturn, 0, false);
				}
				// if eligible members' size is 0, clear the instance
				if (instance.getChars().size() == 0) {
					instance.clear();
				}
			}
		}
	}

	public void setInstanceTime(int seconds) {
		setInstanceTime(seconds, 0, 0);
	}

	public void setInstanceTime(int seconds, int forcedReturnFieldId) {
		Instance instance = chr.getInstance();
		if (instance != null) {
			if (forcedReturnFieldId != 0) {
				instance.setForcedReturn(forcedReturnFieldId);
			}
			if (instance.getRemainingTime() < System.currentTimeMillis()) {
				// don't override old timeout value
				instance.setTimeout(seconds);
			}
		}
	}

	public void setInstanceTime(int seconds, int forcedReturnFieldId, int portalId) {
		Instance instance = chr.getInstance();
		if (instance != null) {
			if (forcedReturnFieldId != 0) {
				instance.setForcedReturn(forcedReturnFieldId);
			}
			if (portalId != 0) {
				instance.setForcedReturnPortalId(portalId);
			}
			if (instance.getRemainingTime() < System.currentTimeMillis()) {
				// don't override old timeout value
				instance.setTimeout(seconds);
			}
		}
	}

	@Override
	public int getReturnField() {
		return returnField;
	}

	@Override
	public void setReturnField(int returnField) {
		this.returnField = returnField;
	}

	@Override
	public void setReturnField() {
		setReturnField(chr.getFieldID());
	}

	@Override
	public boolean hasMobsInField() {
		return hasMobsInField(chr.getFieldID());
	}

	public Mob waitForMobDeath() {
        Object response = null;
        if (isActive(ScriptType.FirstEnterField)) {
            response = getScriptInfoByType(ScriptType.FirstEnterField).awaitResponse();
        } else if (isActive(ScriptType.Field)) {
            response = getScriptInfoByType(ScriptType.Field).awaitResponse();
        }
        if (response == null) {
            throw new NullPointerException(INTENDED_NPE_MSG);
        }
        return (Mob) response;
    }

	public Mob waitForMobDeath(int... possibleMobs) {
		Mob mob = waitForMobDeath();
		while (true) {
			if (mob == null) {
				throw new NullPointerException(INTENDED_NPE_MSG);
			} else {
				for (int mobID : possibleMobs) {
					if (mob.getTemplateId() == mobID) {
						return mob;
					}
				}
				mob = waitForMobDeath();
			}
		}
	}

	@Override
	public boolean hasMobsInField(int fieldid) {
		Field field = chr.getOrCreateFieldByCurrentInstanceType(fieldid);
		return field.getMobs().size() > 0;
	}

	@Override
	public int getAmountOfMobsInField() {
		return getAmountOfMobsInField(chr.getFieldID());
	}

	@Override
	public int getAmountOfMobsInField(int fieldid) {
		Field field = FieldData.getFieldById(fieldid);
		return field.getMobs().size();
	}

	public void killMobs() {
		List<Mob> mobs = new ArrayList<>(chr.getField().getMobs());
		for (Mob mob : mobs) {
			mob.die(false);
		}
	}

	/**
	 * Kill one or all mobs with the given mob ID in the characters map
	 * @param mobId mob id to kill
	 * @param killAll whether or not to kill all of them or just the first one
	 */
	public void killMob(int mobId, boolean killAll) {
		Field field = chr.getOrCreateFieldByCurrentInstanceType(chr.getFieldID());

		for (Mob m : field.getMobs()) {
			if (m.getTemplateId() == mobId) {
				m.die(false);

				if (!killAll)
					return;
			}
		}
	}

	public void showWeatherNoticeToField(String text, WeatherEffNoticeType type) {
		showWeatherNoticeToField(text, type, 7000); // 7 seconds
	}

	public void showWeatherNoticeToField(String text, WeatherEffNoticeType type, int duration) {
		Field field = chr.getField();
		field.broadcastPacket(WvsContext.weatherEffectNotice(type, text, duration));
	}

	public void showEffectToField(String dir) {
		showEffectToField(dir, 0);
	}

	public void showEffectToField(String dir, int delay) {
		Field field = chr.getField();
		field.broadcastPacket(UserPacket.effect(Effect.effectFromWZ(dir, false, delay, 4, 0)));
	}

	public void showFieldEffect(String dir) {
		showFieldEffect(dir, 0);
	}

	@Override
	public void showFieldEffect(String dir, int delay) {
		chr.write(FieldPacket.fieldEffect(FieldEffect.getFieldEffectFromWz(dir, delay)));
	}

	public void showFieldEffectToField(String dir) {
		showFieldEffect(dir, 0);
	}

	public void showFieldEffectToField(String dir, int delay) {
		Field field = chr.getField();
		field.broadcastPacket(FieldPacket.fieldEffect(FieldEffect.getFieldEffectFromWz(dir, delay)));
	}

	public void showFieldBackgroundEffect(String dir) {
		showFieldBackgroundEffect(dir, 0);
	}

	public void showFieldBackgroundEffect(String dir, int delay) {
		Field field = chr.getField();
		chr.write(FieldPacket.fieldEffect(FieldEffect.getFieldBackgroundEffectFromWz(dir, delay)));
	}

	public void showFadeTransition(int duration, int fadeInTime, int fadeOutTime) {
		chr.write(FieldPacket.fieldEffect(FieldEffect.takeSnapShotOfClient2(fadeInTime, duration, fadeOutTime, true)));
	}

	public void showFade(int duration) {
		chr.write(FieldPacket.fieldEffect(FieldEffect.takeSnapShotOfClient(duration)));
	}

	public void setFieldColour(GreyFieldType colorFieldType, short red, short green, short blue, int time) {
		chr.write(FieldPacket.fieldEffect(FieldEffect.setFieldColor(colorFieldType, red, green, blue, time)));
	}

	public void setFieldGrey(GreyFieldType colorFieldType, boolean show) {
		chr.write(FieldPacket.fieldEffect(FieldEffect.setFieldGrey(colorFieldType, show)));
	}

	@Override
	public void dropItem(int itemId, int x, int y) {
		Field field = chr.getField();
		Drop drop = new Drop(field.getNewObjectID());
		drop.setItem(ItemData.getItemDeepCopy(itemId));
		Position position = new Position(x, y);
		drop.setPosition(position);
		field.drop(drop, position, true);
	}

	@Override
	public void teleportInField(Position position) {
		chr.write(FieldPacket.teleport(position, chr));
	}

	@Override
	public void teleportInField(int x, int y) {
		teleportInField(new Position(x, y));
	}

	@Override
	public void teleportToPortal(int portalId) {
		Portal portal = chr.getField().getPortalByID(portalId);
		if (portal != null) {
			Position position = new Position(portal.getX(), portal.getY());
			chr.write(FieldPacket.teleport(position, chr));
		}
	}

	public Drop getDropInRect(int itemID, Rect rect) {
	    Field field = getField();
	    if (field == null) {
	        field = chr.getField();
        }
	    return field.getDropsInRect(rect).stream()
                .filter(drop -> drop.getItem() != null && drop.getItem().getItemId() == itemID)
                .findAny().orElse(null);
    }

    @Override
    public Drop getDropInRect(int itemID, int rectRange) {
        return getDropInRect(itemID, new Rect(
                new Position(
                        chr.getPosition().getX() - rectRange,
                        chr.getPosition().getY() - rectRange),
                new Position(
                        chr.getPosition().getX() + rectRange,
                        chr.getPosition().getY() + rectRange))
        );

    }

	// Life-related methods --------------------------------------------------------------------------------------------


	// NPC methods
	@Override
	public void spawnNpc(int npcId, int x, int y) {
		Npc npc = NpcData.getNpcDeepCopyById(npcId);
		Position position = new Position(x, y);
		npc.setPosition(position);
		npc.setCy(y);
		npc.setRx0(x + 50);
		npc.setRx1(x - 50);
		npc.setFh(chr.getField().findFootHoldBelow(new Position(x, y -2)).getId());
		npc.setNotRespawnable(true);
		if (npc.getField() == null) {
			npc.setField(field);
		}

		chr.getField().spawnLife(npc, chr);
	}

	@Override
	public void removeNpc(int npcId) {
		chr.getField().getNpcs().stream()
				.filter(npc -> npc.getTemplateId() == npcId)
				.findFirst()
				.ifPresent(npc -> chr.getField().removeLife(npc));
	}

	@Override
	public void openNpc(int npcId) {
		Npc npc = NpcData.getNpcDeepCopyById(npcId);
		String script;
		if(npc.getScripts().size() > 0) {
			script = npc.getScripts().get(0);
		} else {
			script = String.valueOf(npc.getTemplateId());
		}
		chr.getScriptManager().startScript(npc.getTemplateId(), npcId, script, ScriptType.Npc);
	}

	@Override
	public void openShop(int shopID) {
		NpcShopDlg nsd = NpcData.getShopById(shopID);
		if (nsd != null) {
			chr.setShop(nsd);
			chr.write(ShopDlg.openShop(0, nsd));
		} else {
			chat(String.format("Could not find shop with id %d.", shopID));
			log.error(String.format("Could not find shop with id %d.", shopID));
		}
	}

	@Override
	public void openTrunk(int npcTemplateID) {
		chr.write(FieldPacket.trunkDlg(new TrunkOpen(npcTemplateID, chr.getAccount().getTrunk())));
	}

	@Override
	public void setSpeakerID(int templateID) {
		NpcScriptInfo nsi = getNpcScriptInfo();
		boolean isNotCancellable = nsi.hasParam(NpcScriptInfo.Param.NotCancellable);
		nsi.resetParam();
		nsi.setOverrideSpeakerTemplateID(templateID);
		if (isNotCancellable) {
			nsi.addParam(NpcScriptInfo.Param.NotCancellable);
		}
	}

	@Override
	public void setSpeakerType(byte speakerType) {
		NpcScriptInfo nsi = getNpcScriptInfo();
		nsi.setSpeakerType(speakerType);
	}

	public void hideNpcByTemplateId(int npcTemplateId, boolean hide) {
		hideNpcByTemplateId(npcTemplateId, hide, hide);
	}

	@Override
	public void hideNpcByTemplateId(int npcTemplateId, boolean hideTemplate, boolean hideNameTag) {
		Field field = chr.getField();
		Life life = field.getLifeByTemplateId(npcTemplateId);
		if(life == null || !(life instanceof Npc)) {
			log.error(String.format("npc %d is null or not an instance of Npc", npcTemplateId));
			return;
		}
		chr.write(NpcPool.npcViewOrHide(life.getObjectId(), !hideTemplate, !hideNameTag));
	}

	public void hideNpcByObjectId(int npcObjId, boolean hide) {
		hideNpcByObjectId(npcObjId, hide, hide);
	}

	@Override
	public void hideNpcByObjectId(int npcObjId, boolean hideTemplate, boolean hideNameTag) {
		Field field = chr.getField();
		Life life = field.getLifeByObjectID(npcObjId);
		if(!(life instanceof Npc)) {
			log.error(String.format("npc %d is null or not an instance of Npc", npcObjId));
			return;
		}
		chr.write(NpcPool.npcViewOrHide(life.getObjectId(), !hideTemplate, !hideNameTag));
	}

	@Override
	public void moveNpcByTemplateId(int npcTemplateId, boolean left, int distance, int speed) {
		Field field = chr.getField();
		Life life = field.getLifeByTemplateId(npcTemplateId);
		if(!(life instanceof Npc)) {
			log.error(String.format("npc %d is null or not an instance of Npc", npcTemplateId));
			return;
		}
		chr.write(NpcPool.npcSetForceMove(life.getObjectId(), left, distance, speed));
	}

	@Override
	public void moveNpcByObjectId(int npcObjId, boolean left, int distance, int speed) {
		Field field = chr.getField();
		Life life = field.getLifeByObjectID(npcObjId);
		if(!(life instanceof Npc)) {
			log.error(String.format("npc %d is null or not an instance of Npc", npcObjId));
			return;
		}
		chr.write(NpcPool.npcSetForceMove(life.getObjectId(), left, distance, speed));
	}

	@Override
	public void flipNpcByTemplateId(int npcTemplateId, boolean left) {
		Field field = chr.getField();
		Life life = field.getLifeByTemplateId(npcTemplateId);
		if(!(life instanceof Npc)) {
			log.error(String.format("npc %d is null or not an instance of Npc", npcTemplateId));
			return;
		}
		chr.write(NpcPool.npcSetForceFlip(life.getObjectId(), left));
	}

	@Override
	public void flipNpcByObjectId(int npcObjId, boolean left) {
		Field field = chr.getField();
		Life life = field.getLifeByObjectID(npcObjId);
		if(!(life instanceof Npc)) {
			log.error(String.format("npc %d is null or not an instance of Npc", npcObjId));
			return;
		}
		chr.write(NpcPool.npcSetForceFlip(life.getObjectId(), left));
	}

	public void showNpcSpecialActionByTemplateId(int npcTemplateId, String effectName) {
		showNpcSpecialActionByTemplateId(npcTemplateId, effectName, 0);
	}

	@Override
	public void showNpcSpecialActionByTemplateId(int npcTemplateId, String effectName, int duration) {
		Field field = chr.getField();
		Life life = field.getLifeByTemplateId(npcTemplateId);
		if(!(life instanceof Npc)) {
			log.error(String.format("npc %d is null or not an instance of Npc", npcTemplateId));
			return;
		}
		chr.write(NpcPool.npcSetSpecialAction(life.getObjectId(), effectName, duration));
	}

	public void showNpcSpecialActionByObjectId(int npcObjId, String effectName) {
		showNpcSpecialActionByObjectId(npcObjId, effectName, 0);

	}

	@Override
	public void showNpcSpecialActionByObjectId(int npcObjId, String effectName, int duration) {
		Field field = chr.getField();
		Life life = field.getLifeByObjectID(npcObjId);
		if(!(life instanceof Npc)) {
			log.error(String.format("npc %d is null or not an instance of Npc", npcObjId));
			return;
		}
		chr.write(NpcPool.npcSetSpecialAction(life.getObjectId(), effectName, duration));
	}

	public int getNpcObjectIdByTemplateId(int npcTemplateId) {
		Field field = chr.getField();
		Life life = field.getLifeByTemplateId(npcTemplateId);
		if(!(life instanceof Npc)) {
			log.error(String.format("npc %d is null or not an instance of Npc", npcTemplateId));
			return 0;
		}
		return life.getObjectId();
	}



	// Mob methods
	@Override
	public Mob spawnMob(int id) {
		return spawnMob(id, 0, 0, false);
	}

	@Override
	public Mob spawnMob(int id, boolean respawnable) {
		return spawnMob(id, 0, 0, respawnable);
	}

	@Override
	public Mob spawnMobOnChar(int id) {
		return spawnMob(id, chr.getPosition().getX(), chr.getPosition().getY(), false);
	}

	@Override
	public Mob spawnMobOnChar(int id, boolean respawnable) {
		return spawnMob(id, chr.getPosition().getX(), chr.getPosition().getY(), respawnable);
	}

	@Override
	public Mob spawnMob(int id, int x, int y, boolean respawnable) {
		return spawnMob(id, x, y, respawnable, 0);
	}

	public Mob spawnMob(int id, int x, int y, boolean respawnable, long hp) {
		return chr.getField().spawnMob(id, x, y, respawnable, hp);
	}

	public Mob spawnMobWithAppearType(int id, int x, int y, int appearType, int option) {
		return chr.getField().spawnMobWithAppearType(id, x, y, appearType, option);
	}

	@Override
	public void removeMobByObjId(int id) {
		chr.getField().removeLife(id);
		chr.getField().broadcastPacket(MobPool.leaveField(id, DeathType.ANIMATION_DEATH));
	}

	@Override
	public void removeMobByTemplateId(int id) {
		Field field = chr.getField();
		Life life = field.getLifeByTemplateId(id);
		if(life == null) {
			log.error(String.format("Could not find Mob by template id %d.", id));
			return;
		}
		removeMobByObjId(life.getObjectId());
	}

    public boolean isFinishedEscort(int templateID) {
        Field field = chr.getField();
        Life life = field.getLifeByTemplateId(templateID);
        if(!(life instanceof Mob)) {
            WvsContext.dispose(chr);
            return false;
        }
        Mob mob = (Mob) life;
        boolean finished = mob.isFinishedEscort();
        if (!finished) {
            WvsContext.dispose(chr);
        }
        return finished;
    }

	@Override
	public void showHP(int templateID) {
		chr.getField().getMobs().stream()
				.filter(m -> m.getTemplateId() == templateID)
				.findFirst()
				.ifPresent(mob -> chr.getField().broadcastPacket(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(mob))));
	}

	@Override
	public void showHP() {
		chr.getField().getMobs().stream()
				.filter(m -> m.getHp() > 0)
				.findFirst()
				.ifPresent(mob -> chr.getField().broadcastPacket(FieldPacket.fieldEffect(FieldEffect.mobHPTagFieldEffect(mob))));
	}



	// Reactor methods
	@Override
	public void removeReactor() {
		Field field = chr.getField();
		Life life = field.getLifeByObjectID(getObjectIDByScriptType(ScriptType.Reactor));
		if (life instanceof Reactor) {
			field.removeLife(life.getObjectId(), false);
		}
	}

	@Override
	public void spawnReactor(int reactorId, int x, int y) {
		Field field = chr.getField();
		Reactor reactor = ReactorData.getReactorByID(reactorId);
		Position position = new Position(x, y);
		reactor.setPosition(position);
		field.addLife(reactor);
	}

	public void spawnReactorInState(int reactorId, int x, int y, byte state) {
		Field field = chr.getField();
		Reactor reactor = ReactorData.getReactorByID(reactorId);
		reactor.setState(state);
		Position position = new Position(x, y);
		reactor.setPosition(position);
		field.addLife(reactor);
		chr.write(ReactorPool.reactorEnterField(reactor));
	}

	@Override
	public boolean hasReactors() {
		Field field = chr.getField();
		return field.getReactors().size() > 0;
	}

	@Override
	public int getReactorQuantity() {
		Field field = chr.getField();
		return field.getReactors().size();
	}


	public int getReactorState(int reactorId) {
		Field field = chr.getField();
		Life life = field.getLifeByTemplateId(reactorId);
		if (life != null && life instanceof Reactor) {
			Reactor reactor = (Reactor) life;
			return reactor.getState();
		}
		return -1;
	}

	public void increaseReactorState(int reactorId, int stateLength) {
		chr.getField().increaseReactorState(chr, reactorId, stateLength);
	}

	public void changeReactorState(int reactorId, byte state, short delay, byte stateLength) {
		Field field = chr.getField();
		Reactor reactor = field.getReactors().stream()
						.filter(r -> r.getObjectId() == getObjectIDByScriptType(ScriptType.Reactor))
						.findAny().orElse(null);
		if (reactor == null) {
			return;
		}
		reactor.setState(state);
		chr.write(ReactorPool.reactorChangeState(reactor, delay, stateLength));
	}



	// Party-related methods -------------------------------------------------------------------------------------------

	@Override
	public Party getParty() {
		return chr.getParty();
	}

	@Override
	public int getPartySize() {return getParty().getMembers().size();}

	@Override
	public void setPartyField() {
		chr.setFieldInstanceType(FieldInstanceType.PARTY);
	}

	@Override
	public void setChannelField() {
		chr.setFieldInstanceType(FieldInstanceType.CHANNEL);
	}

	@Override
	public boolean isPartyLeader() {
		return chr.getParty() != null && chr.getParty().getPartyLeaderID() == chr.getId();
	}

	@Override
	public boolean checkParty() {
		if (chr.getParty() == null) {
			chat("You are not in a party.");
			return false;
		} else if (!isPartyLeader()) {
			chat("You are not the party leader.");
			return false;
		}
		boolean res = true;
		Char leader = chr.getParty().getPartyLeader().getChr();
		if (leader == null) {
			chat("Your leader is currently offline.");
		} else {
			int fieldID = leader.getFieldID();
			for (PartyMember pm : chr.getParty().getPartyMembers()) {
				if (pm != null) {
					res &= pm.getChr() != null && pm.isOnline() && pm.getFieldID() == fieldID;
				}
			}
		}
		if (!res) {
			chat("Make sure that your whole party is online and in the same map as the party leader.");
		}
		return res;
	}

	public List<Char> getOnlinePartyMembers() {
		Party party = getParty();
		if (party == null) {
			return new ArrayList<>();
		}
		return party.getOnlineChars();
	}

	public List<Char> getPartyMembersInSameField(Char chr) {
		Party party = getParty();
		if (party == null) {
			return new ArrayList<>();
		}
		List<Char> list = new ArrayList<>(party.getPartyMembersInSameField(chr));
		list.add(chr);
		return new ArrayList<>(list);
	}

	public void resetPartyQRValue(int qId) {
		setPartyQRValue(qId, "0");
	}

	public void setPartyQRValue(int qId, String value) {
		for (Char c : chr.getParty().getOnlineChars()) {
			createQuestWithQRValue(c, qId, value, true);
		}
	}

	// Guild/Alliance related methods -------------------------------------------------------------------------------------------

	@Override
	public void showGuildCreateWindow() {
		chr.write(WvsContext.guildResult(GuildResult.msg(GuildType.Req_InputGuildName)));
	}

	@Override
	public boolean checkAllianceName(String name) {
		World world = chr.getClient().getWorld();
		return world.getAlliance(name) == null;
	}

	public void incrementMaxGuildMembers(int amount) {
		Guild guild = chr.getGuild();
		guild.setMaxMembers(guild.getMaxMembers() + amount);
		guild.broadcast(WvsContext.guildResult(GuildResult.incMaxMemberNum(guild)));
	}

	public void createAlliance(String name, Char other) {
		Alliance alliance = new Alliance();
		alliance.setName(name);
		alliance.addGuild(chr.getGuild());
		alliance.addGuild(other.getGuild());
		GuildMember chrMember = chr.getGuild().getMemberByCharID(chr.getId());
		chrMember.setAllianceGrade(1);
		GuildMember otherMember = other.getGuild().getMemberByCharID(other.getId());
		otherMember.setAllianceGrade(2);
		DatabaseManager.saveToDB(alliance);
		chr.getGuild().setAlliance(alliance);
		other.getGuild().setAlliance(alliance);
		alliance.broadcast(WvsContext.allianceResult(AllianceResult.createDone(alliance)));
		chr.deductMoney(5000000);
	}



	// Chat-related methods --------------------------------------------------------------------------------------------

	@Override
	public void chat(String text) {
		chatRed(text);
	}

	@Override
	public void chatRed(String text) {
		chr.chatMessage(SystemNotice, text);
	}

	@Override
	public void chatBlue(String text) {
		chr.chatMessage(Notice2, text);
	}

	public void systemMessage(String message) {
		chr.write(WvsContext.message(MessageType.SYSTEM_MESSAGE, 0, message, (byte) 0));
	}

	@Override
	public void chatScript(String text) {chr.chatScriptMessage(text);}

	public void showWeatherNotice(String text, WeatherEffNoticeType type) {
		showWeatherNotice(text, type, 7000); // 7 seconds
	}

	@Override
	public void showWeatherNotice(String text, WeatherEffNoticeType type, int duration) {
		chr.write(WvsContext.weatherEffectNotice(type, text, duration));
	}



	// Inventory-related methods ---------------------------------------------------------------------------------------

	@Override
	public void giveMesos(long mesos) {
		chr.addMoney(mesos);
		chr.write(WvsContext.incMoneyMessage((int) mesos));
	}

	@Override
	public void deductMesos(long mesos) {
		chr.deductMoney(mesos);
		chr.write(WvsContext.incMoneyMessage((int) -mesos));
	}

	@Override
	public long getMesos() {
		return chr.getMoney();
	}

	@Override
	public void giveItem(int id) {
		giveItem(id, 1);
	}

	@Override
	public void giveItem(int id, int quantity) {
		chr.addItemToInventory(id, quantity);
	}

	public void giveAndEquip(int id) {
		if (!ItemConstants.isEquip(id)) {
			giveItem(id);
		}
		Item equip = ItemData.getItemDeepCopy(id);
		if (equip == null) {
			return;
		}
		// replace the old equip if there was any
		Inventory equipInv = chr.getEquipInventory();
		int bodyPart = ItemConstants.getBodyPartFromItem(id, chr.getAvatarData().getAvatarLook().getGender());
		Item oldEquip = equipInv.getItemBySlot((short) bodyPart);
		if (oldEquip != null) {
			chr.unequip(oldEquip);
			oldEquip.updateToChar(chr);
		}
		equip.setBagIndex(bodyPart);
		chr.equip(equip);
		equip.updateToChar(chr);
	}

	@Override
	public boolean hasItem(int id) {
		return hasItem(id, 1);
	}

	@Override
	public boolean isEquipped(int id) { return chr.getInventoryByType(InvType.EQUIPPED).getItems().stream().filter(item -> item.getItemId() == id).count() > 0; }

	@Override
	public boolean hasItem(int id, int quantity) {
		return getQuantityOfItem(id) >= quantity;
	}

	public void consumeItem() {
		consumeItem(getParentID());
	}

	@Override
	public void consumeItem(int itemID) {
		chr.consumeItem(itemID, 1);
	}

	@Override
	public void consumeItem(int itemID, int amount) {
		chr.consumeItem(itemID, amount);
	}

	@Override
	public void useItem(int id) {
		ItemBuffs.giveItemBuffsFromItemID(chr, chr.getTemporaryStatManager(), id);
	}

	@Override
	public int getQuantityOfItem(int id) {
		if (ItemConstants.isEquip(id)) {
			Item equip = chr.getInventoryByType(InvType.EQUIP).getItemByItemID(id);
			if (equip == null) {
				return 0;
			}
			return equip.getQuantity();
		} else {
			Item item2 = ItemData.getItemDeepCopy(id);
			InvType invType = item2.getInvType();
			Item item = chr.getInventoryByType(invType).getItemByItemID(id);
			if (item == null) {
				return 0;
			}
			return item.getQuantity();
		}
	}

	@Override
	public boolean canHold(int id) {
		return chr.canHold(id);
	}

	@Override
	public boolean canHold(int id, int quantity) {
		return chr.canHold(id, quantity);
	}

	@Override
	public int getEmptyInventorySlots(InvType invType) {
		return chr.getInventoryByType(invType).getEmptySlots();
	}

    public int getEmptyInventorySlots(int invType) {
        return chr.getInventoryByType(InvType.getInvTypeByVal(invType)).getEmptySlots();
    }


	// Quest-related methods -------------------------------------------------------------------------------------------

	@Override
	public void completeQuest(int id) {
		if (hasQuest(id) && isComplete(id)) {
			chr.getQuestManager().completeQuest(id);
		}
	}

	@Override
	public void completeQuestNoRewards(int id) {
		QuestManager qm = chr.getQuestManager();
		Quest quest = qm.getQuests().get(id);
		if (quest == null) {
			quest = QuestData.createQuestFromId(id);
		}
		quest.setCompletedTime(FileTime.currentTime());
		quest.setStatus(QuestStatus.Completed);
		qm.addQuest(quest);
		chr.write(WvsContext.questRecordMessage(quest));
		chr.chatMessage(String.format("Quest %d completed by completeQuestNoRewards", id));
	}

	@Override
	public void startQuestNoCheck(int id) {
		QuestManager qm = chr.getQuestManager();
		qm.addQuest(QuestData.createQuestFromId(id));
		chr.chatMessage(String.format("Quest %d started by startQuestNoCheck", id));
	}

	@Override
	public void startQuest(int id) {
		QuestManager qm = chr.getQuestManager();
		if (qm.canStartQuest(id)) {
			qm.addQuest(QuestData.createQuestFromId(id));
		}
	}

	@Override
	public boolean hasQuest(int id) {
		return chr.getQuestManager().hasQuestInProgress(id);
	}

	@Override
	public boolean hasQuestCompleted(int id) {
		return chr.getQuestManager().hasQuestCompleted(id);
	}

	public void createQuestWithQRValue(int questId, String qrValue, boolean ex) {
		createQuestWithQRValue(chr, questId, qrValue, ex);
	}

	public void createQuestWithQRValue(int questId, String qrValue) {
		createQuestWithQRValue(chr, questId, qrValue, true);
	}

	public void createQuestWithQRValue(Char character, int questId, String qrValue, boolean ex) {
		QuestManager qm = character.getQuestManager();
		Quest quest = qm.getQuests().get(questId);
		if (quest == null) {
			quest = new Quest(questId, QuestStatus.Started);
			quest.setQrValue(qrValue);
			qm.addCustomQuest(quest);
		}
		quest.setQrValue(qrValue);
		updateQRValue(questId, ex);
	}

	public void deleteQuest(int questId) {
		deleteQuest(chr, questId);
	}

	public void deleteQuest(Char character, int questId) {
		QuestManager qm = chr.getQuestManager();
		Quest quest = qm.getQuests().get(questId);
		if(quest == null) {
			return;
		}
		qm.removeQuest(quest.getQRKey());
	}

	public String getQRValue(int questId) {
		return getQRValue(chr, questId);
	}

	public String getQRValue(Char character, int questId) {
		Quest quest = chr.getQuestManager().getQuests().get(questId);
		if (quest == null) {
			return "Quest is Null";
		}
		return quest.getQRValue();
	}

	public void setQRValue(int questId, String qrValue) { setQRValue(questId, qrValue, true);}

	public void setQRValue(int questId, String qrValue, boolean ex) {
		setQRValue(chr, questId, qrValue, ex);
	}

	public void setQRValue(Char character, int questId, String qrValue, boolean ex) {
		Quest quest = chr.getQuestManager().getQuests().get(questId);
		quest.setQrValue(qrValue);
		updateQRValue(questId, ex);
	}

	public void addQRValue(int questId, String qrValue) {
		addQRValue(questId, qrValue, true);
	}

	public void addQRValue(int questId, String qrValue, boolean ex) {
		String qrVal = getQRValue(questId);
		if (qrVal.equals("") || qrVal.equals("Quest is Null")) {
			createQuestWithQRValue(questId, qrValue);
			return;
		}
		setQRValue(questId, qrValue + ";" + qrVal);
		updateQRValue(questId, ex);
	}

	public boolean isComplete(int questID) {
		return chr.getQuestManager().isComplete(questID);
	}

	public void updateQRValue(int questId, boolean ex) {
		Quest quest = chr.getQuestManager().getQuests().get(questId);
		if (quest == null) {
			log.error(String.format("The user does not have the quest %d.", questId));
			return;
		}
		if (ex) {
			chr.write(WvsContext.questRecordExMessage(quest));
		} else {
			chr.write(WvsContext.questRecordMessage(quest));
		}
	}



	// Party Quest-related methods -------------------------------------------------------------------------------------

	public void incrementMonsterParkCount() {
		chr.setMonsterParkCount( (byte) (chr.getMonsterParkCount() + 1));
	}

	public byte getMonsterParkCount() {
		return chr.getMonsterParkCount();
	}

	public String getDay() {
		return new SimpleDateFormat("EEEE", Locale.ENGLISH).format(System.currentTimeMillis());
	}

	public int getMPExpByMobId(int templateId) {
		return MonsterPark.getExpByMobId(templateId);
	}

	public int getMPReward() {
		return MonsterPark.getRewardByDay();
	}

	public long getPQExp() {
		return getPQExp(chr);
	}

	public long getPQExp(Char chr) {
		return GameConstants.PARTY_QUEST_EXP_FORMULA(chr);
	}



	// Boss-related methods --------------------------------------------------------------------------------------------

	@Override
	public void setDeathCount(int deathCount) {
		chr.setDeathCount(deathCount);
		chr.write(UserLocal.deathCountInfo(deathCount));
	}

	@Override
	public void setPartyDeathCount(int deathCount) {
		if (chr.getParty() != null) {
			for (PartyMember pm : chr.getParty().getOnlineMembers()) {
				pm.getChr().setDeathCount(deathCount);
			}
		}
	}

	public void createObstacleAtom(ObtacleAtomEnum oae, int key, int damage, int velocity, int amount, int proc) {
		createObstacleAtom(oae, key, damage, velocity, 0, amount, proc);
	}

	@Override
	public void createObstacleAtom(ObtacleAtomEnum oae, int key, int damage, int velocity, int angle, int amount, int proc) {
		Field field = chr.getField();
		int xLeft = field.getVrLeft();
		int yTop = field.getVrTop();

		ObtacleInRowInfo obtacleInRowInfo = new ObtacleInRowInfo(4, false, 5000, 0, 0, 0);
		ObtacleRadianInfo obtacleRadianInfo = new ObtacleRadianInfo(4, 0, 0, 0, 0);
		Set<ObtacleAtomInfo> obtacleAtomInfosSet = new HashSet<>();

		for(int i = 0; i < amount; i++) {
			if(Util.succeedProp(proc)) {
				int randomX = new Random().nextInt(field.getWidth()) + xLeft;
				Position position = new Position(randomX, yTop);
				Foothold foothold = field.findFootHoldBelow(position);
				if (foothold != null) {
					int footholdY = foothold.getYFromX(position.getX());
					int height = position.getY() - footholdY;
					height = height < 0 ? -height : height;

					obtacleAtomInfosSet.add(new ObtacleAtomInfo(oae.getType(), key, position, new Position(), oae.getHitBox(),
							damage, 0, 0, height, 0, velocity, height, angle));
				}
			}
		}

		field.broadcastPacket(FieldPacket.createObtacle(ObtacleAtomCreateType.NORMAL, obtacleInRowInfo, obtacleRadianInfo, obtacleAtomInfosSet));
	}

	public void stopEvents() {
		Set<ScheduledFuture> events = getEvents();
		events.forEach(st -> st.cancel(true));
		events.clear();
		Field field;
		if (chr != null) {
			field = chr.getField();
		} else {
			field = this.field;
		}
		field.broadcastPacket(FieldPacket.clock(ClockPacket.removeClock()));
	}

	private Set<ScheduledFuture> getEvents() {
		return events;
	}

	public void addEvent(ScheduledFuture event) {
		getEvents().add(event);
	}

	// Character Temporary Stat-related methods ------------------------------------------------------------------------

	@Override
	public void giveCTS(CharacterTemporaryStat cts, int nOption, int rOption, int time) {
		TemporaryStatManager tsm = chr.getTemporaryStatManager();
		Option o = new Option();
		o.nOption = nOption;
		o.rOption = rOption;
		o.tOption = time;
		tsm.putCharacterStatValue(cts, o);
		tsm.sendSetStatPacket();
	}

	@Override
	public void removeCTS(CharacterTemporaryStat cts) {
		TemporaryStatManager tsm = chr.getTemporaryStatManager();
		tsm.removeStat(cts, false);
	}

	@Override
	public void removeBuffBySkill(int skillId) {
		TemporaryStatManager tsm = chr.getTemporaryStatManager();
		tsm.removeStatsBySkill(skillId);
	}

	@Override
	public boolean hasCTS(CharacterTemporaryStat cts) {
		TemporaryStatManager tsm = chr.getTemporaryStatManager();
		return tsm.hasStat(cts);
	}

	@Override
	public int getnOptionByCTS(CharacterTemporaryStat cts) {
		TemporaryStatManager tsm = chr.getTemporaryStatManager();
		return hasCTS(cts) ? tsm.getOption(cts).nOption : 0;
	}

	@Override
	public void rideVehicle(int mountID) {
		TemporaryStatManager tsm = chr.getTemporaryStatManager();
		TemporaryStatBase tsb = tsm.getTSBByTSIndex(TSIndex.RideVehicle);

		tsb.setNOption(mountID);
		tsb.setROption(0);
		tsm.putCharacterStatValue(RideVehicle, tsb.getOption());
		tsm.sendSetStatPacket();
	}



	// InGameDirectionEvent methods ------------------------------------------------------------------------------------

	@Override
	public int moveCamera(boolean back, int speed, int x, int y) {
		getNpcScriptInfo().setMessageType(AskIngameDirection);
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.cameraMove(back, speed, new Position(x, y))));
        Object response = getScriptInfoByType(getLastActiveScriptType()).awaitResponse();
        if (response == null) {
            throw new NullPointerException(INTENDED_NPE_MSG);
        }
		return (int) response;
	}

	public void moveCamera(int speed, int x, int y) {
		moveCamera(false, speed, x, y);
	}

	public void moveCameraBack(int speed) {
		moveCamera(true, speed, chr.getPosition().getX(), chr.getPosition().getY());
	}

	@Override
	public int zoomCamera(int inZoomDuration, int scale, int x, int y) {
		getNpcScriptInfo().setMessageType(AskIngameDirection);
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.cameraZoom(inZoomDuration, scale, 1000, new Position(x, y))));
        Object response = getScriptInfoByType(getLastActiveScriptType()).awaitResponse();
        if (response == null) {
            throw new NullPointerException(INTENDED_NPE_MSG);
        }
		return (int) response;
	}

	@Override
	public void resetCamera() {
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.cameraOnCharacter(0))); // 0 resets the Camera
	}

	public void setCameraOnNpc(int npcTemplateId) {
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.cameraOnCharacter(npcTemplateId)));
	}

	@Override
	public int sendDelay(int delay) {
		getNpcScriptInfo().setMessageType(AskIngameDirection);
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.delay(delay)));
		Object response = getScriptInfoByType(getLastActiveScriptType()).awaitResponse();
		if (response == null) {
			throw new NullPointerException(INTENDED_NPE_MSG);
		}
		return (int) response;
	}

	@Override
	public void doEventAndSendDelay(int delay, String methodName, Object...args) {
		invoke(chr.getScriptManager(), methodName, args);
		sendDelay(delay);
	}

	@Override
	public void forcedMove(boolean left, int distance) {
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.forcedMove(left, distance)));
	}

	@Override
	public void forcedFlip(boolean left) {
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.forcedFlip(left)));
	}

	@Override
	public void forcedAction(int type, int duration) {
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.forcedAction(type, duration)));
	}

	@Override
	public void forcedInput(int type) {
		ForcedInputType fit = ForcedInputType.getByVal(type);
		if (fit == null) {
			log.error(String.format("Unknown Forced Input Type %d", type));
			return;
		}
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.forcedInput(type)));
	}

	public void patternInputRequest(String pattern, int act, int requestCount, int time) {
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.patternInputRequest(pattern, act, requestCount, time)));
	}

	@Override
	public void hideUser(boolean hide) {
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.vansheeMode(hide)));
	}

	public void showEffect(String path, int duration, int x, int y) {
		showEffect(path, duration, x, y, 0, 0, true, 0);
	}

	@Override
	public void showEffect(String path, int duration, int x, int y, int z, int npcIdForExtend, boolean onUser, int idk2) {
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.effectPlay(path, duration, new Position(x, y), z, npcIdForExtend, onUser, idk2)));
	}

	public void showEffectOnPosition(String path, int duration, int x, int y) {
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.effectPlay(path, duration,
				new Position(x, y), 0, 1, false, 0)));
	}

	public void showBalloonMsgOnNpc(String path, int duration, int x, int y, int templateID) {
		int objectID = getNpcObjectIdByTemplateId(templateID);
		if (objectID == 0) return;
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.effectPlay(path, duration,
				new Position(x, y), 0, objectID, false, 0)));
	}
	public void showBalloonMsgOnNpc(String path, int duration, int templateID) {
		showBalloonMsgOnNpc(path, duration, 0, -100, templateID);
	}

	public void showNpcEffectOnPosition(String path, int x, int y, int templateID) {
		int objectID = getNpcObjectIdByTemplateId(templateID);
		if (objectID == 0) return;
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.effectPlay(path, 0,
				new Position(x, y), 0, objectID, false, 0)));
	}

	public void showBalloonMsg(String path, int duration) {
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.effectPlay(path, duration,
				new Position(0, -100), 0, 0, true, 0)));
	}

	public int sayMonologue(String text, boolean isEnd) {
        getNpcScriptInfo().setMessageType(Monologue);
        chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.monologue(text, isEnd)));
        Object response = getScriptInfoByType(getLastActiveScriptType()).awaitResponse();
        if (response == null) {
            throw new NullPointerException(INTENDED_NPE_MSG);
        }
        return (int) response;
	}

	public void avatarLookSet(int[] equipIDs) {
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.avatarLookSet(equipIDs)));
	}

	public void faceOff(int faceItemID) {
		chr.write(UserLocal.inGameDirectionEvent(InGameDirectionEvent.faceOff(faceItemID)));
	}

	// Clock methods ---------------------------------------------------------------------------------------------------

	public Clock createStopWatch(int seconds) {
		return new Clock(ClockType.StopWatch, chr.getField(), seconds);
	}

	public Clock createClock(int seconds) {
		return new Clock(ClockType.SecondsClock, chr.getField(), seconds);
	}

	public void createClock(int hours, int minutes, int seconds) {
		chr.write(FieldPacket.clock(ClockPacket.hmsClock((byte) hours, (byte) minutes, (byte) seconds)));
		addEvent(EventManager.addEvent(this::removeClock, seconds + minutes * 60 + hours * 3600, TimeUnit.SECONDS));
	}

	public void createClockForMultiple(int seconds, int... fieldIDs) {
	    for (int fieldID : fieldIDs) {
	        Field field = chr.getOrCreateFieldByCurrentInstanceType(fieldID);
	        new Clock(ClockType.SecondsClock, field, seconds);
        }
    }

	public void removeClock() {
		chr.write(FieldPacket.clock(ClockPacket.removeClock()));
	}

	public Clock createTimerGauge(int seconds) {
		return new Clock(ClockType.TimerGauge, chr.getField(), seconds);
	}



	// Other methods ---------------------------------------------------------------------------------------------------

	@Override
	public boolean addDamageSkin(int itemID) {
		Account acc = chr.getAccount();
		DamageSkinType error = null;
		if (acc.getDamageSkins().size() >= GameConstants.DAMAGE_SKIN_MAX_SIZE) {
			error = DamageSkinType.Res_Fail_SlotCount;
		} else if (acc.getDamageSkinByItemID(itemID) != null) {
			error = DamageSkinType.Res_Fail_AlreadyExist;
		}
		if (error != null) {
			chr.write(UserLocal.damageSkinSaveResult(DamageSkinType.Req_Reg, error, null));
		} else {
			QuestManager qm = chr.getQuestManager();
			Quest q = qm.getQuests().getOrDefault(QuestConstants.DAMAGE_SKIN, null);
			if (q == null) {
				q = new Quest(QuestConstants.DAMAGE_SKIN, QuestStatus.Started);
				qm.addQuest(q);
			}
			chr.consumeItem(itemID, 1);
			DamageSkinSaveData dssd = DamageSkinSaveData.getByItemID(itemID);
			q.setQrValue(String.valueOf(dssd.getDamageSkinID()));
			acc.addDamageSkin(dssd);
			chr.setDamageSkin(dssd);
			chr.write(UserLocal.damageSkinSaveResult(DamageSkinType.Req_Reg,
					DamageSkinType.Res_Success, chr));
			chr.write(UserPacket.setDamageSkin(chr));
			chr.write(WvsContext.questRecordMessage(q));
		}
		return error == null;
	}

	@Override
	public void openUI(UIType uiID){
		int uiIDValue = uiID.getVal();
		chr.write(FieldPacket.openUI(uiIDValue));
	}

	@Override
	public void showClearStageExpWindow(long expGiven) {
		chr.write(FieldPacket.fieldEffect(FieldEffect.showClearStageExpWindow((int) expGiven)));
		giveExpNoMsg(expGiven);
	}

	public void removeBlowWeather() {
	    chr.write(FieldPacket.removeBlowWeather());
    }

	public void blowWeather(int itemID, String message) {
        removeBlowWeather();// removing old one if exists.
	    chr.write(FieldPacket.blowWeather(itemID, message));
    }

    public void playSound(String sound) { playSound(sound, 100); }// default
	public void playSound(String sound, int vol) {
		chr.write(FieldPacket.fieldEffect(FieldEffect.playSound(sound, vol)));
	}

	public void blind(int enable, int x, int color, int time) { blind(enable, x, color, 0, 0, time); }

	public void blind(int enable, int x, int color, int unk1, int unk2, int time) { chr.write(FieldPacket.fieldEffect(FieldEffect.blind(enable, x, color, unk1, unk2, time))); }
	@Override
	public int getRandomIntBelow(int upBound) {
		return new Random().nextInt(upBound);
	}

	public void showEffect(String dir) {
		showEffect(dir, 0);
	}

	public void showEffect(String dir, int delay) {
		showEffect(dir, 4, delay);
	}

	public void showScene(String xmlPath, String sceneName, String sceneNumber) {
		Scene scene = new Scene(chr, xmlPath, sceneName, sceneNumber);
		scene.createScene();
	}

	@Override
	public void showEffect(String dir, int placement, int delay) {
		chr.write(UserPacket.effect(Effect.effectFromWZ(dir, false, delay, placement, 0)));
	}

	public void avatarOriented(String effectPath) {
		chr.write(UserPacket.effect(Effect.avatarOriented(effectPath)));
	}

	public void reservedEffect(String effectPath) {
		chr.write(UserPacket.effect(Effect.reservedEffect(effectPath)));

		String[] splitted = effectPath.split("/");
		String sceneName = splitted[splitted.length - 2];
		String sceneNumber = splitted[splitted.length - 1];
		String xmlPath = effectPath.replace("/" + sceneName, "").replace("/" + sceneNumber, "").replace("Effect/", "Effect.wz/");

		int fieldID = new Scene(chr, xmlPath, sceneName, sceneNumber).getTransferField();
		if (fieldID != 0) {
			chr.setTransferField(fieldID);
		}
	}

	public void reservedEffectRepeat(String effectPath, boolean start) { chr.write(UserPacket.effect(Effect.reservedEffectRepeat(effectPath, start))); }

	public void reservedEffectRepeat(String effectPath) { reservedEffectRepeat(effectPath, true); }

	public void playExclSoundWithDownBGM(String soundPath, int volume) { chr.write(UserPacket.effect(Effect.playExclSoundWithDownBGM(soundPath, volume))); }

	public void blindEffect(boolean blind) { chr.write(UserPacket.effect(Effect.blindEffect(blind))); }

	public void fadeInOut(int fadeIn, int delay, int fadeOut, int alpha) {
		chr.write(UserPacket.effect(Effect.fadeInOut(fadeIn, delay, fadeOut, alpha)));
	}

	public String formatNumber(String number) {
		return Util.formatNumber(number);
	}

	private Object invoke(Object invokeOn, String methodName, Object... args) {
		List<Class<?>> classList = Arrays.stream(args).map(Object::getClass).collect(Collectors.toList());
		Class<?>[] classes = classList.stream().map(Util::convertBoxedToPrimitiveClass).toArray(Class<?>[]::new);
		Method func;
		try {
			func = getClass().getMethod(methodName, classes);
			return func.invoke(invokeOn, args);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void invokeForParty(String methodName, Object... args) {
		for (PartyMember pm : chr.getParty().getMembers()) {
			boolean fromDB = false;
			Char chr = pm.getChr();
			if (chr == null) {
				chr = Char.getFromDBById(pm.getCharID());
				fromDB = true;
			}
			invoke(chr.getScriptManager(), methodName, args);
			if (fromDB) {
				DatabaseManager.saveToDB(chr);
			}
		}
	}

	public ScheduledFuture invokeAfterDelay(long delay, String methodName, Object...args) {
		ScheduledFuture sf =  EventManager.addEvent(() -> invoke(this, methodName, args), delay);
		addEvent(sf);
		return sf;
	}

	public ScheduledFuture invokeAtFixedRate(long initialDelay, long delayBetweenExecutions,
											 int executes, String methodName, Object...args) {
		ScheduledFuture scheduledFuture;
		if (executes == 0) {
			scheduledFuture = EventManager.addFixedRateEvent(() -> invoke(this, methodName, args), initialDelay,
					delayBetweenExecutions);
		} else {
			scheduledFuture = EventManager.addFixedRateEvent(() -> invoke(this, methodName, args), initialDelay,
					delayBetweenExecutions, executes);
		}
		addEvent(scheduledFuture);
		return scheduledFuture;
	}

	@Override
	public int playVideoByScript(String videoPath) {
		getNpcScriptInfo().setMessageType(PlayMovieClip);
		chr.write(UserLocal.videoByScript(videoPath, false));
		Object response = getScriptInfoByType(getLastActiveScriptType()).awaitResponse();
		if (response == null) {
			throw new NullPointerException(INTENDED_NPE_MSG);
		}
		return (int) response;
	}

	public void setFuncKeyByScript(boolean add, int action, int key) {
		chr.write(UserLocal.setFuncKeyByScript(add, action, key));
		chr.getFuncKeyMap().putKeyBinding(key, add ? (byte) 1 : (byte) 0, action);
	}

	public void addPopUpSay(int npcID, int duration, String message, String effect) {
		chr.write(UserLocal.addPopupSay(npcID, duration, message, effect));
	}

	public void moveParticleEff(String type, int startX, int startY, int endX, int endY, int moveTime, int totalCount, int oneSprayMin, int oneSprayMax) {
		chr.write(UserLocal.moveParticleEff(type, new Position(startX, startY), new Position(endX, endY), moveTime, totalCount, oneSprayMin, oneSprayMax));
	}

	public void levelUntil(int toLevel) {
		short level = chr.getLevel();
		if (level >= toLevel) {
		    return;
        }
		while (level < toLevel) {
			addLevel(1);
			level++;
		}
	}

	public void ballonMsg(String message) {
		chr.write(UserLocal.balloonMsg(message, 100, 3, null));
	}

	public void hireTutor(boolean set) { chr.hireTutor(set); }

	public void tutorAutomatedMsg(int id) { tutorAutomatedMsg(id, 10000); }

	public void tutorAutomatedMsg(int id, int duration) { chr.tutorAutomatedMsg(id, duration); }

	public void tutorCustomMsg(String message, int width, int duration) { chr.tutorCustomMsg(message, width, duration); }

	public boolean hasTutor() { return chr.hasTutor(); }

	public int getMakingSkillLevel(int skillID) { return chr.getMakingSkillLevel(skillID); }

	public boolean isAbleToLevelUpMakingSkill(int skillID) {
		int neededProficiency = SkillConstants.getNeededProficiency(chr.getMakingSkillLevel(skillID));
		if (neededProficiency <= 0) {
			return false;
		}
		return chr.getMakingSkillProficiency(skillID) >= neededProficiency;
	}

	public void makingSkillLevelUp(int skillID) { chr.makingSkillLevelUp(skillID); }

	private ScriptMemory getMemory() {
		return memory;
	}

	public void openGolluxPortal(String action, int show) {
		chr.getField().broadcastPacket(FieldPacket.golluxOpenPortal(chr, action, show));
	}

	public void addClearedGolluxMap() {
		chr.getOrCreateFieldByCurrentInstanceType(BossConstants.GOLLUX_FIRST_MAP).getProperties().put(String.valueOf(chr.getFieldID()), 2);
		updateGolluxMap();
	}

	public void addCurrentGolluxMap() {
		if (chr.getField() == null || chr.getField().getMobs() == null) {
			return;
		}
		int type = chr.getField().getMobs().size() == 0 ? 2 : 1;
		chr.getOrCreateFieldByCurrentInstanceType(BossConstants.GOLLUX_FIRST_MAP).getProperties().put(String.valueOf(chr.getFieldID()), type);
		updateGolluxMap();
	}

	public void updateGolluxMap() {
		chr.getField().broadcastPacket(FieldPacket.golluxUpdateMiniMap(chr));
	}

	public boolean golluxMapAlreadyVisited() {
		return chr.getOrCreateFieldByCurrentInstanceType(BossConstants.GOLLUX_FIRST_MAP).getProperties().keySet().contains(String.valueOf(chr.getFieldID()));
	}

	public GolluxDifficultyType getGolluxDifficulty() {
		Map<String, Object> golluxMaps = chr.getOrCreateFieldByCurrentInstanceType(BossConstants.GOLLUX_FIRST_MAP).getProperties();
		byte difficulty = 3;
		ArrayList<Integer> golluxMainParts = new ArrayList<>();
		golluxMainParts.add(BossConstants.GOLLUX_ABDOMEN);
		golluxMainParts.add(BossConstants.GOLLUX_RIGHT_SHOULDER);
		golluxMainParts.add(BossConstants.GOLLUX_LEFT_SHOULDER);
		for (Map.Entry<String, Object> entry : golluxMaps.entrySet()) {
			if (golluxMainParts.contains(Integer.valueOf(entry.getKey())) && Integer.valueOf(entry.getValue().toString()) == 2) {
				difficulty--;
			}
		}
		return GolluxDifficultyType.getByVal(difficulty);
	}

	public void spawnGollux(byte phase) {
		if (phase > 2) {
			return;
		}
		int mobId = 9390600 + phase;
		Mob gollux = MobData.getMobDeepCopyById(mobId);
		int hpMultiplier = BossConstants.GOLLUX_HP_MULTIPLIERS[phase][getGolluxDifficulty().getVal()];
		Mob mob = spawnMob(mobId, 0, 0, false, gollux.getHp() * Long.valueOf(hpMultiplier));
		blockGolluxAttacks();
	}

	public void blockGolluxAttacks() {
		Mob mob = null;
		for (int i = 9390600; i <= 9390602; i++) {
			mob = (Mob) chr.getField().getLifeByTemplateId(i);
			if (mob != null) {
				break;
			}
		}
		if (mob == null) {
			return;
		}
		Map<String, Object> golluxMaps = chr.getOrCreateFieldByCurrentInstanceType(BossConstants.GOLLUX_FIRST_MAP).getProperties();
		ArrayList<Integer> blockedSkills = new ArrayList<>();
		if ((int) golluxMaps.getOrDefault(String.valueOf(BossConstants.GOLLUX_RIGHT_SHOULDER), 0) == 2) {
			blockedSkills.addAll(Arrays.stream(BossConstants.GOLLUX_RIGHT_HAND_SKILLS).boxed().collect(Collectors.toList()));
		}
		if ((int) golluxMaps.getOrDefault(String.valueOf(BossConstants.GOLLUX_LEFT_SHOULDER), 0) == 2) {
			blockedSkills.addAll(Arrays.stream(BossConstants.GOLLUX_LEFT_HAND_SKILLS).boxed().collect(Collectors.toList()));
		}
		if ((int) golluxMaps.getOrDefault(String.valueOf(BossConstants.GOLLUX_ABDOMEN), 0) == 2) {
			blockedSkills.add(BossConstants.GOLLUX_BREATH_ATTACK);
		}
		mob.getField().broadcastPacket(MobPool.mobAttackBlock(mob, blockedSkills));
	}

	public void changeFootHold(String footHoldName, boolean show) {
		chr.getField().broadcastPacket(FieldPacket.footholdAppear(footHoldName, show));
	}

	public boolean hasMobById(int mobID) {
		return chr.getField().getLifeByTemplateId(mobID) != null;
	}

	public void clearGolluxClearedMaps() {
		chr.getOrCreateFieldByCurrentInstanceType(BossConstants.GOLLUX_FIRST_MAP).getProperties().clear();
	}

	public void spawnMobRespawnable(int id, int x, int y, boolean respawnable, long hp, int respawnTime) {
		chr.getField().spawnMobRespawnable(id, x, y, respawnable, hp, respawnTime);
	}

	public void createFallingCatcherOnCharacter(String name) {
		ArrayList<Position> positions = new ArrayList<Position>();
		positions.add(chr.getPosition());
		chr.getField().broadcastPacket(FieldPacket.createFallingCatcher(name, 1, 1, positions));
	}

	public void getItemsFromTrunkEmployee() {
		chr.getItemsFromEmployeeTrunk();
	}

	public void spawnLotus(byte phase, byte difficulty) {
		if (phase > 2 || difficulty > 1) {
			return;
		}
		int lotusId = BossConstants.LOTUS_MOBID + phase; // phases start from 0 to 2
		long hp = BossConstants.LOTUS_HP_PHASE_DIFFICULTY[phase][difficulty];
		Mob lotus = spawnMob(lotusId, 0, -16, false, hp);
		if (phase == 0) {
			MobSkillInfo msi = SkillData.getMobSkillInfoByIdAndLevel(MobSkillID.LaserAttack.getVal(), 1);
			MobSkill mobSkill = new MobSkill();
			mobSkill.setLevel(5); //at this level there are 4 lasers and 100% damr
			mobSkill.setSkillID(MobSkillID.LaserAttack.getVal());
			mobSkill.setFixDamR(msi.getSkillStatIntValue(fixDamR));
			mobSkill.applyEffect(lotus);
		}
	}

	public void addStorageSlots(byte amount) {
		chr.getAccount().getTrunk().addSlots(amount);
	}

	public void addInventorySlotsByInvType(byte amount, byte type) {
		chr.getInventoryByType(InvType.getInvTypeByVal(type)).addSlots(amount);
	}

	public int getSlotsLeftToAddByInvType(byte type) {
		return GameConstants.MAX_INVENTORY_SLOTS - chr.getInventoryByType(InvType.getInvTypeByVal(type)).getSlots();
	}


	// only for items with quantity
	public void dropItem(int itemId, int itemQuantity, Mob deadMob) {
		Field field = chr.getField();
		Drop drop = new Drop(field.getNewObjectID());
		drop.setItem(ItemData.getItemDeepCopy(itemId));
		if (ItemConstants.isEquip(itemId)) {
			drop.getItem().setQuantity(itemQuantity);
		}
		field.drop(drop, deadMob.getPosition());
	}

	public void dropItem(int itemId, int startPosX, int startPosY, int endPosX, int endPosY) {
		Field field = chr.getField();
		Drop drop = new Drop(field.getNewObjectID());
		drop.setItem(ItemData.getItemDeepCopy(itemId));
		Position startPos = new Position(startPosX, startPosY);
		Position endPos = new Position(endPosX, endPosY);
		field.drop(drop, startPos, endPos, true);
	}

	public void dropMeso(int mesoAmount, int startPosX, int startPosY, int endPosX, int endPosY) {
		Field field = chr.getField();
		Drop drop = new Drop(field.getNewObjectID(), mesoAmount);
		Position startPos = new Position(startPosX, startPosY);
		Position endPos = new Position(endPosX, endPosY);
		field.drop(drop, startPos, endPos, true);
	}

	public void spawnZakum(int map) {
		short pX = BossConstants.ZAKUM_SPAWN_X;
		short pY = BossConstants.ZAKUM_SPAWN_Y;
		int zakBody = BossConstants.ZAKUM_EASY_BODY;
		int zakArm = BossConstants.ZAKUM_EASY_ARM;
		switch (map) {
			case BossConstants.ZAKUM_HARD_ALTAR:
				zakBody = BossConstants.ZAKUM_HARD_BODY;
				zakArm = BossConstants.ZAKUM_HARD_ARM;
				break;
			case BossConstants.ZAKUM_CHAOS_ALTAR:
				zakBody = BossConstants.ZAKUM_CHAOS_BODY;
				zakArm = BossConstants.ZAKUM_CHAOS_ARM;
				break;
		}

		spawnMob(zakArm, pX, pY, false);
		for (int i = 1; i <= 8; i++) {
			spawnMob(zakBody + i, pX, pY, false);
		}

		chr.getOrCreateFieldByCurrentInstanceType(map).setProperty("zakum", 1);
	}

	public void spawnBalrog(boolean easy) {
		int[] spawns = {
				easy ? BossConstants.BALROG_EASY_BODY : BossConstants.BALROG_HARD_BODY,
				easy ? BossConstants.BALROG_EASY_LARM : BossConstants.BALROG_HARD_LARM,
				easy ? BossConstants.BALROG_EASY_RARM : BossConstants.BALROG_HARD_RARM,
				easy ? BossConstants.BALROG_EASY_DMGSINK : BossConstants.BALROG_HARD_DMGSINK,
		};

		for (int i = 0; i < spawns.length; i++) {
			spawnMob(spawns[i], BossConstants.BALROG_SPAWN_X, BossConstants.BALROG_SPAWN_Y, false);
		}
	}

	public void resetBossMap(int fieldId) {
		Field map = chr.getOrCreateFieldByCurrentInstanceType(fieldId);

		if (map.getClock() != null) {
			map.getClock().removeClock();
		}
		for (Mob m : map.getMobs()) {
			map.removeLife(m);
		}
		for (Drop d : map.getDrops()) {
			map.removeLife(d);
		}
		for (Char c : map.getChars()) {
			c.setDeathCount(0);
			c.write(UserLocal.deathCountInfo(0));
		}
	}

	/**
	 * Don't save return map on warp
	 *
	 * @param id  fieldId
	 * @param pid portalId
	 */
	public void warpNoReturn(int id, int pid) {
		chr.warp(id, pid, false);
	}

	public boolean zakumAlreadySpawned(int map) {
		field = chr.getClient().getChannelInstance().getFieldIfExists(map);
		return field != null && field.getProperties().containsKey("zakum") && field.getProperties().get("zakum").equals("1");
	}

	/**
	 * ObjectID variable gets passed to script in constructor but is not accessible in this class.
	 * This function solves that.
	 * Doesn't work with map scripts.
	 *
	 * @return the map object ID of the script owner instance.
	 */
	public int getObjectID() {
		return objectID;
	}
	public int getObjectPositionX() {
		return chr.getField().getLifeByObjectID(getObjectID()).getX();
	}

	public int getObjectPositionY() {
		return chr.getField().getLifeByObjectID(getObjectID()).getY();
	}

	public void setDisableDropsInMap(int fieldId, boolean onoff) {
		Field map = chr.getOrCreateFieldByCurrentInstanceType(fieldId);
		map.setDropsDisabled(onoff);
	}

	public void sendAutoEventClock() {
		InGameEvent ige = InGameEventManager.getInstance().getActiveEvent();

		if (ige == null) {
			return;
		}

		if (ige.isActive()) {
			ige.sendLobbyClock(chr);
		}
	}

	public boolean isRouletteActive() {
		return InGameEventManager.getInstance().getActiveEvent() instanceof RussianRouletteEvent;
	}

	public boolean isPinkZakumActive() {
		return InGameEventManager.getInstance().getActiveEvent() instanceof PinkZakumEvent;
	}

	public void returnPinkZakum() {
		InGameEvent e = InGameEventManager.getInstance().getActiveEvent();

		int warpMap = e instanceof PinkZakumEvent
				? PinkZakumEvent.BATTLE_MAP
				: chr.getPreviousFieldID();

		chr.warp(warpMap, 0, false);
	}

	public boolean isPinkZakumOpen() {
		return isPinkZakumActive() && InGameEventManager.getInstance().getOpenEvent() instanceof PinkZakumEvent;
	}

	public int getPreviousFieldID() {
		return chr.getPreviousFieldID();
	}

	public boolean isPinkZakumWinner() {
		PinkZakumEvent pze = ((PinkZakumEvent)InGameEventManager.getInstance().getEvent(InGameEventType.PinkZakumBattle));
		return pze.isWinner(chr) && !pze.getWinnerRewarded(chr);
	}

	public int getPreviousPortalID() {
		return chr.getPreviousPortalID();
	}

	public boolean canWarpSilentCrusade(int targetFieldId) {
		return chr.getClient().getChannelInstance().tryEnterSilentCrusadePortal(chr, targetFieldId, chr.getClient().getChannelInstance().getChannelId());
	}

	public boolean canWarpAreaBoss(int targetFieldId) {
		return chr.getClient().getChannelInstance().canWarpAreaBoss(chr, targetFieldId, chr.getClient().getChannelInstance().getChannelId());
	}

	public void trySpawnAreaBoss() {
		chr.getClient().getChannelInstance().trySpawnAreaBoss(chr, getFieldID(), chr.getClient().getChannelInstance().getChannelId());
	}

	public void overrideAreaBossTimer(int targetFieldId) {
		chr.getClient().getChannelInstance().overrideAreaBossTimer(targetFieldId, chr.getClient().getChannelInstance().getChannelId());
	}
}
