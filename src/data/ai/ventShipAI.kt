
package com.dp.advancedgunnerycontrol.weaponais.shipais


import com.dp.advancedgunnerycontrol.weaponais.shipais.ShipCommandGenerator
import com.dp.advancedgunnerycontrol.weaponais.shipais.ShipCommandWrapper
import com.dp.advancedgunnerycontrol.weaponais.getAverageArmor
import com.dp.advancedgunnerycontrol.weaponais.getMaxArmor
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.ShipAIPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipCommand
import org.lazywizard.lazylib.combat.CombatUtils
import org.lazywizard.lazylib.ext.minus
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

//note: modified version of Jannes' ventShipAI, exists to tune the likeliness of vent behavior (probably upwards)

class VentShipAI(ship: ShipAPI, private val fluxThreshold : Float = 0.5f,
                 private val safetyFactor : Float = 1.5f, private val aggressive : Boolean = false)
    : ShipCommandGenerator(ship) {
    private var isSafe = false
    private var frameTracker = 0
    private var wasVenting = false
    private var shouldReevaluate = false
    private var blockCommands = listOf<ShipCommand>()

    companion object {
        const val scanningRange = 2000f
        const val checkFrequency = 20
    }

    private fun isSafeToVent(): Boolean {
        // val nearbyEnemies = CombatUtils.getShipsWithinRange(ship.location, scanningRange).filterNotNull().filter { it.owner == 1 }
        val nearbyEnemiesAndAllies =
            CombatUtils.getShipsWithinRange(ship.location, scanningRange).filterNotNull().filter { it.owner != 100 }
                .partition { it.owner == 1 }
        val nearbyEnemies = nearbyEnemiesAndAllies.first
        val nearbyAllies = nearbyEnemiesAndAllies.second
        val enemyMissiles = CombatUtils.getMissilesWithinRange(ship.location, scanningRange).filterNotNull().filter { it.owner == 1 }
        val armorGrid = ship.armorGrid ?: return false
        val armor = getAverageArmor(armorGrid)
        val armorIntegrity = (armor / getMaxArmor(armorGrid)) *  ship.hullLevel
        val health = armorIntegrity * armor + ship.hitpoints

        var danger = 0f
        nearbyEnemies.forEach { enemy ->
            enemy.allWeapons.forEach { weapon ->
                val dangerFactor = computeDangerFactor(weapon.damageType, armorIntegrity) *
                        min(1f, (weapon.range / (ship.location - weapon.location).length()).pow(2)) *
                        (if(weapon.usesAmmo() && weapon.ammo == 0) 0f else 1f) *
                        (if(weapon.spec?.primaryRoleStr?.toLowerCase() == "finisher") 2f else 1f)
                danger += (weapon.derivedStats?.dps?: 0f) * dangerFactor
            }
        }

        enemyMissiles.forEach {
            danger += it.damageAmount * computeDangerFactor(it.damageType, armorIntegrity)
        }

        danger /= sqrt(0.01f + nearbyAllies.size.toFloat())

        return health / safetyFactor > danger * (ship.fluxTracker?.timeToVent?: 1f)
    }

    private fun computeDangerFactor(type: DamageType, armorIntegrity: Float) : Float{
        return when (type) {
            DamageType.KINETIC -> armorIntegrity.pow(2) * 0.5f + (1f - armorIntegrity.pow(2)) * 0.9f
            DamageType.HIGH_EXPLOSIVE -> armorIntegrity.pow(2) * 2f + (1f - armorIntegrity.pow(2)) * 1.2f
            DamageType.FRAGMENTATION -> armorIntegrity.pow(2) * 0.25f + (1f - armorIntegrity.pow(2)) * 0.7f
            DamageType.ENERGY -> 1f
            else -> 1f
        }
    }

    override fun generateCommands(): List<ShipCommandWrapper> {
        shouldReevaluate = false
        blockCommands = listOf()
        if(ship.fluxTracker?.isVenting == true){
            wasVenting = true
            if(aggressive) {
                blockCommands = listOf(ShipCommand.ACCELERATE_BACKWARDS)
                return listOf(ShipCommandWrapper(ShipCommand.ACCELERATE, ship.shipTarget?.location))
            }
            return emptyList()
        }

        if(wasVenting){
            wasVenting = false
            shouldReevaluate = true
        }

        if (ship.fluxLevel >= fluxThreshold) {
            frameTracker--
            if(frameTracker <= 0){
                frameTracker = checkFrequency
                isSafe = isSafeToVent()
            }
            if(isSafe) {
                return listOf(ShipCommandWrapper(ShipCommand.VENT_FLUX))
            }
        }else{
            frameTracker = 0
        }
        return emptyList()
    }

    override fun shouldReevaluate(): Boolean = shouldReevaluate
    override fun blockCommands(): List<ShipCommand> = blockCommands
}