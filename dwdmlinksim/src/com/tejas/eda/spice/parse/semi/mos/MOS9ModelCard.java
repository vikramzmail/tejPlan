/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tejas.eda.spice.parse.semi.mos;

import com.tejas.eda.spice.device.semi.mos.MOS9Instance;
import com.tejas.eda.spice.device.semi.mos.MOS9ModelValues;
import com.tejas.eda.spice.parse.Deck;
import com.tejas.eda.spice.parse.ParserException;

import static com.tejas.eda.spice.Constants.CtoK;

/**
 * @author Kristopher T. Beck
 */
public class MOS9ModelCard extends MOSModelCard<MOS9Instance, MOS9ModelValues> {

    public MOS9ModelCard(String cardString) throws ParserException {
        super(cardString);
    }

    @Override
    public MOS9Instance createInstance() {
        MOS9Instance instance = new MOS9Instance();
        initModelValues(instance);
        return instance;
    }

    @Override
    public void setProperty(MOS9ModelValues model, String name, String value) {
        if (name.equals("VTO")) {
            model.setVt0(parseDouble(value));
        } else if (name.equals("KP")) {
            model.setTransconductance(parseDouble(value));
        } else if (name.equals("GAMMA")) {
            model.setGamma(parseDouble(value));
        } else if (name.equals("PHI")) {
            model.setPhi(parseDouble(value));
        } else if (name.equals("RD")) {
            model.setDrnResistance(parseDouble(value));
        } else if (name.equals("RS")) {
            model.setSrcResistance(parseDouble(value));
        } else if (name.equals("CBD")) {
            model.setCapBD(parseDouble(value));
        } else if (name.equals("CBS")) {
            model.setCapBS(parseDouble(value));
        } else if (name.equals("IS")) {
            model.setJctSatCur(parseDouble(value));
        } else if (name.equals("PB")) {
            model.setBlkJctPotential(parseDouble(value));
        } else if (name.equals("CGSO")) {
            model.setGateSrcOverlapCapFactor(parseDouble(value));
        } else if (name.equals("CGDO")) {
            model.setGateDrnOverlapCapFactor(parseDouble(value));
        } else if (name.equals("CGBO")) {
            model.setGateBlkOverlapCapFactor(parseDouble(value));
        } else if (name.equals("RSH")) {
            model.setSheetResistance(parseDouble(value));
        } else if (name.equals("CJ")) {
            model.setBlkCapFactor(parseDouble(value));
        } else if (name.equals("MJ")) {
            model.setBlkJctBotGradingCoeff(parseDouble(value));
        } else if (name.equals("CJSW")) {
            model.setSideWallCapFactor(parseDouble(value));
        } else if (name.equals("MJSW")) {
            model.setBlkJctSideGradingCoeff(parseDouble(value));
        } else if (name.equals("JS")) {
            model.setJctSatCurDensity(parseDouble(value));
        } else if (name.equals("TOX")) {
            model.setOxideThickness(parseDouble(value));
        } else if (name.equals("LD")) {
            model.setLatDiff(parseDouble(value));
        } else if (name.equals("XL")) {
            model.setLengthAdjust(parseDouble(value));
        } else if (name.equals("WD")) {
            model.setWidthNarrow(parseDouble(value));
        } else if (name.equals("XW")) {
            model.setWidthAdjust(parseDouble(value));
        } else if (name.equals("DELVTO")) {
            model.setDelvt0(parseDouble(value));
        } else if (name.equals("U0")) {
            model.setSurfaceMobility(parseDouble(value));
        } else if (name.equals("FC")) {
            model.setFwdCapDepCoeff(parseDouble(value));
        } else if (name.equals("NSUB")) {
            model.setSubstrateDoping(parseDouble(value));
        } else if (name.equals("TPG")) {
            model.setGateType(Integer.parseInt(value));
        } else if (name.equals("NSS")) {
            model.setSurfaceStateDensity(parseDouble(value));
        } else if (name.equals("ETA")) {
            model.setEta(parseDouble(value));
        } else if (name.equals("DELTA")) {
            model.setDelta(parseDouble(value));
        } else if (name.equals("NFS")) {
            model.setFastSurfaceStateDensity(parseDouble(value));
        } else if (name.equals("THETA")) {
            model.setTheta(parseDouble(value));
        } else if (name.equals("VMAX")) {
            model.setMaxDriftVel(parseDouble(value));
        } else if (name.equals("KAPPA")) {
            model.setKappa(parseDouble(value));
        } else if (name.equals("XJ")) {
            model.setJunctionDepth(parseDouble(value));
        } else if (name.equals("TNOM")) {
            model.setTnom(parseDouble(value) + CtoK);
        } else if (name.equals("KF")) {
            model.setfNcoef(parseDouble(value));
        } else if (name.equals("AF")) {
            model.setfNexp(parseDouble(value));
        }
    }
}
