package data.hullmods

import com.fs.starfarer.api.combat.BaseHullMod
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc

class lortus_vent_hullmod : BaseHullMod() {

    var maxSpeedBonusWhilePseudoVenting: Map<HullSize, Float> = mapOf(
        HullSize.DEFAULT to 0.0F,
        HullSize.FIGHTER to 200.0F,
        HullSize.FRIGATE to 200.0F,
        HullSize.DESTROYER to 160.0F,
        HullSize.CRUISER to 120.0F,
        HullSize.CAPITAL_SHIP to 80.0F
    )

    //note: needs refinement through testing

    var accelBoostMap: Map<HullSize, Float> = mapOf(
        HullSize.DEFAULT to 0.0F,
        HullSize.FIGHTER to 20.0F,
        HullSize.FRIGATE to 20.0F,
        HullSize.DESTROYER to 20.0F,
        HullSize.CRUISER to 20.0F,
        HullSize.CAPITAL_SHIP to 20.0F
    )

    var zeroFluxBoostNotVentingBonus = 50F

    override fun applyEffectsBeforeShipCreation(hullSize: HullSize, stats: MutableShipStatsAPI, id: String) {
        stats.acceleration.modifyMult(
            id,
            accelBoostMap!![hullSize]!!
        )
        stats.deceleration.modifyMult(
            id,
            accelBoostMap!![hullSize]!!
        )
        stats.turnAcceleration.modifyMult(
            id,
            accelBoostMap!![hullSize]!!
        )
        stats.zeroFluxSpeedBoost.modifyFlat(
            id,
            zeroFluxBoostNotVentingBonus
        )
    }

    override fun isApplicableToShip(ship: ShipAPI): Boolean {
        return true
    }

    override fun getUnapplicableReason(ship: ShipAPI): String? {
        return null
    }

    //TODO feactor some of this shit into a separate "jank towards enemy ship" AI module"

    override fun advanceInCombat(ship: ShipAPI, amount: Float) {

        if (!ship.isAlive) {
            return
        }

        //TODO increase odds of venting depending on ship personality

        if(ship.fluxTracker.isVenting){
            val timeForVent =ship.fluxTracker.getTimeToVent();
            ship.setCustomData("pseudoVentTimeLeft", timeForVent);
            //ship.mutableStats.maxSpeed.modifyFlat(ship.id, accelBoostMap[ship.hullSize]!! - zeroFluxBoostNotVentingBonus)
            ship.fluxTracker.stopVenting();
        }
        val pseudoVentTimeLeft : Float = ship.getCustomData().get("ship.getCustomData()") as Float;
        if(pseudoVentTimeLeft>0){

            //note: change stats&disable weapons each frame as if the ship were really venting.

            ship.fluxTracker.decreaseFlux((ship.mutableStats.fluxDissipation.modifiedValue)*amount);
            ship.setCustomData("pseudoVentTimeLeft", pseudoVentTimeLeft-amount);
            //TODO disable weapons
        }

        //note: if enemy ship is in range, start janking towards them
        if(ship.areSignificantEnemiesInRange()){
            //TODO: randomly try to jank diagonally left towards or diagonally right towards



            //TODO: some kind of detection for if a collision is immminent and need to try something else

            //note: I guess that breaks down into turn left then accelerate or turn right and then accelerate, or strafe left or right, or accelerate backwards
        }
    }
//-zeroFluxBoostNotVentingBonus

    override fun getDescriptionParam(index: Int, hullSize: HullSize): String? {
        return null
    }

    override fun shouldAddDescriptionToTooltip(hullSize: HullSize, ship: ShipAPI, isForModSpec: Boolean): Boolean {
        return false
    }

    override fun addPostDescriptionSection(
        tooltip: TooltipMakerAPI,
        hullSize: HullSize,
        ship: ShipAPI,
        width: Float,
        isForModSpec: Boolean
    ) {
        val pad = 3f
        val opad = 10f
        val m = Misc.getMissileMountColor()
        val e = Misc.getEnergyMountColor()
        val b = Misc.getHighlightColor()
        val l = Misc.getDesignTypeColor("Low Tech")
        val md = Misc.getDesignTypeColor("Midline")
        val h = Misc.getDesignTypeColor("High Tech")
        val prt = Misc.getDesignTypeColor("Pirate")
        val bad = Misc.getNegativeHighlightColor()
        var label = tooltip.addPara(
            "Increases %s while venting. Adds a new behavior for AI controlled ships, which may vent aggressively while strafing towards the enemy. The chance of AI performing aggressive venting maneuvers depends on the personality type governing the ship: timid ships will never perform such a move, while reckless ships will attempt to do so whenever possible.",
            opad,
            md,
            "" + "vent speed"
        )
        label.setHighlight("" + "vent speed")
        label.setHighlightColors(b)
        tooltip.addSectionHeading("Modifies:", Alignment.MID, opad)
        label = tooltip.addPara(
            "%s while venting.", opad, md,
            "" + "+100% speed"
        )
        label.setHighlight("" + "+100% speed")
        label.setHighlightColors(b)
    }
}



