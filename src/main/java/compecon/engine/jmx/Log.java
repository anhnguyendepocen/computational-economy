/*
This file is part of ComputationalEconomy.

ComputationalEconomy is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

ComputationalEconomy is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with ComputationalEconomy. If not, see <http://www.gnu.org/licenses/>.
 */

package compecon.engine.jmx;

import java.util.Date;
import java.util.Map;

import compecon.culture.sectors.financial.BankAccount;
import compecon.culture.sectors.financial.Currency;
import compecon.culture.sectors.household.Household;
import compecon.culture.sectors.industry.Factory;
import compecon.culture.sectors.state.law.bookkeeping.BalanceSheet;
import compecon.engine.Agent;
import compecon.engine.jmx.model.ModelRegistry;
import compecon.engine.time.ITimeSystemEvent;
import compecon.engine.time.TimeSystem;
import compecon.nature.materia.GoodType;

public class Log {

	private static Agent agentSelectedByClient;

	private static Agent agentCurrentlyActive;

	// --------

	public static boolean isAgentSelectedByClient(Agent agent) {
		return agent != null && agentSelectedByClient == agent;
	}

	public static Agent getAgentSelectedByClient() {
		return agentSelectedByClient;
	}

	public static void setAgentSelectedByClient(Agent agent) {
		agentSelectedByClient = agent;
	}

	public static void setAgentCurrentlyActive(Agent agent) {
		agentCurrentlyActive = agent;
	}

	// --------

	public static void notifyTimeSystem_nextDay(Date date) {
		ModelRegistry.getAgentDetailModel().notifyListeners();
		ModelRegistry.getBalanceSheetsModel().nextPeriod();
		ModelRegistry.getMonetaryTransactionsModel().nextPeriod();
		ModelRegistry.getCapacityModel().nextPeriod();
		ModelRegistry.getEffectiveProductionOutputModel().nextPeriod();
		ModelRegistry.getMoneySupplyM0Model().nextPeriod();
		ModelRegistry.getMoneySupplyM1Model().nextPeriod();
		ModelRegistry.getMoneySupplyM2Model().nextPeriod();
		ModelRegistry.getPricesModel().nextPeriod();
		ModelRegistry.getUtilityModel().nextPeriod();
	}

	// --------

	public static synchronized void log(Agent agent, String message) {
		setAgentCurrentlyActive(agent);
		log(message);
	}

	public static synchronized void log(Agent agent,
			Class<? extends ITimeSystemEvent> eventClass, String message) {
		setAgentCurrentlyActive(agent);
		log(eventClass.getSimpleName() + ": " + message);
	}

	public static void log(String message) {
		if (agentCurrentlyActive != null
				&& agentSelectedByClient == agentCurrentlyActive)
			ModelRegistry.getAgentDetailModel().logAgentEvent(
					TimeSystem.getInstance().getCurrentDate(), message);
	}

	public static void agent_onConstruct(Agent agent) {
		ModelRegistry.getAgentDetailModel().agent_onConstruct(agent);
		if (isAgentSelectedByClient(agent))
			log(agent, agent + " constructed");
		ModelRegistry.getNumberOfAgentsModel().agent_onConstruct(
				agent.getClass());
	}

	public static void agent_onDeconstruct(Agent agent) {
		ModelRegistry.getAgentDetailModel().agent_onDeconstruct(agent);
		if (isAgentSelectedByClient(agent))
			log(agent, agent + " deconstructed");
		ModelRegistry.getNumberOfAgentsModel().agent_onDeconstruct(
				agent.getClass());
	}

	public static void agent_onPublishBalanceSheet(Agent agent,
			BalanceSheet balanceSheet) {
		ModelRegistry.getBalanceSheetsModel().agent_onPublishBalanceSheet(
				agent, balanceSheet);
	}

	// --------

	public static void household_onUtility(Household household,
			Currency currency, Map<GoodType, Double> bundleOfGoodsToConsume,
			double utility) {
		ModelRegistry.getUtilityModel().add(currency, utility);
	}

	public static void household_LabourHourCapacity(Household household,
			double labourHourCapacity) {
		ModelRegistry.getCapacityModel().add(GoodType.LABOURHOUR,
				labourHourCapacity);
	}

	// --------

	public static void factory_onProduction(Factory factory, GoodType goodType,
			double producedProducts) {
		ModelRegistry.getEffectiveProductionOutputModel().add(goodType,
				producedProducts);
	}

	public static void factory_onLabourHourExhaust(Factory factory,
			double amount) {
		ModelRegistry.getEffectiveProductionOutputModel().add(
				GoodType.LABOURHOUR, amount);
	}

	// --------

	public static void bank_onTransfer(BankAccount from, BankAccount to,
			Currency currency, double value, String subject) {
		ModelRegistry.getMonetaryTransactionsModel().bank_onTransfer(
				from.getOwner().getClass(), to.getOwner().getClass(), currency,
				value);
		if (isAgentSelectedByClient(from.getOwner())) {
			String message = from + " --- " + Currency.round(value) + " "
					+ currency.getIso4217Code() + " ---> " + to + ": "
					+ subject;
			ModelRegistry.getAgentDetailModel().logBankAccountEvent(
					TimeSystem.getInstance().getCurrentDate(), from, message);
		}
		if (isAgentSelectedByClient(to.getOwner())) {
			String message = to + " <--- " + Currency.round(value) + " "
					+ currency.getIso4217Code() + " --- " + from + ": "
					+ subject;
			ModelRegistry.getAgentDetailModel().logBankAccountEvent(
					TimeSystem.getInstance().getCurrentDate(), to, message);
		}
	}

	// --------

	public static void centralBank_KeyInterestRate(Currency currency,
			double keyInterestRate) {
		ModelRegistry.getKeyInterestRateModel().add(currency, keyInterestRate);
	}

	public static void centralBank_PriceIndex(Currency currency,
			double priceIndex) {
		ModelRegistry.getPriceIndexModel().add(currency, priceIndex);
	}

	// --------

	public static void market_onTick(double pricePerUnit, GoodType goodType,
			Currency currency, double amount) {
		ModelRegistry.getPricesModel().market_onTick(pricePerUnit, goodType,
				currency, amount);
	}

	public static void market_onTick(double pricePerUnit,
			Currency commodityCurrency, Currency currency, double amount) {
		ModelRegistry.getPricesModel().market_onTick(pricePerUnit,
				commodityCurrency, currency, amount);
	}
}