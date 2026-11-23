package com.example.domain.calc

object CalcRegistry {
    val modules: List<CalcModule> = listOf(
        UnitConversionCalcModule(),
        QuantityTakeoffCalcModule(),
        StructuralBeamCalcModule(),
        StructuralColumnCalcModule(),
        StructuralSlabCalcModule(),
        StructuralLoadComboCalcModule(),
        ConcreteRebarCalcModule(),
        GeotechFootingCalcModule(),
        HydrologyRationalCalcModule(),
        HydrologyManningCalcModule(),
        SurveyingLevelingCalcModule(),
        SurveyingAreaVolumeCalcModule()
    )

    fun byId(id: CalcModuleId): CalcModule =
        modules.first { it.id == id }

    fun byIdString(raw: String?): CalcModule? =
        CalcModuleId.fromId(raw)?.let { id -> modules.firstOrNull { it.id == id } }

    fun byCategory(category: CalcCategory): List<CalcModule> =
        modules.filter { it.category == category }
}
