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

package compecon.culture.markets;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import compecon.culture.markets.ordertypes.MarketOrder;
import compecon.culture.sectors.financial.Bank;
import compecon.culture.sectors.financial.BankAccount;
import compecon.culture.sectors.financial.Currency;
import compecon.culture.sectors.state.law.property.Property;
import compecon.culture.sectors.state.law.property.PropertyRegister;
import compecon.engine.Agent;
import compecon.engine.jmx.Log;
import compecon.engine.util.MathUtil;
import compecon.nature.materia.GoodType;

/**
 * The settlement market is a special market that transfers ownership of offered
 * goods and money, automatically.
 */
public class SettlementMarket extends Market {

	public interface ISettlementEvent {
		public void onEvent(GoodType goodType, double amount,
				double pricePerUnit, Currency currency);

		public void onEvent(Currency commodityCurrency, double amount,
				double pricePerUnit, Currency currency);

		public void onEvent(Property property, double pricePerUnit,
				Currency currency);
	}

	protected Map<Agent, ISettlementEvent> settlementEventListeners = new HashMap<Agent, ISettlementEvent>();

	public void placeSettlementSellingOffer(GoodType goodType, Agent offeror,
			BankAccount offerorsBankAcount, double amount, double pricePerUnit,
			ISettlementEvent settlementEvent) {
		if (amount > 0) {
			this.placeSellingOffer(goodType, offeror, offerorsBankAcount,
					amount, pricePerUnit);
			if (settlementEvent != null)
				this.settlementEventListeners.put(offeror, settlementEvent);
		}
	}

	/**
	 * Place settlement selling offer for a certain amount of money.
	 * 
	 * @param commodityCurrency
	 *            Currency of money to be offered, e.g. EURO.
	 * @param offeror
	 * @param offerorsBankAcount
	 *            Bank account of offeror; offerorsBankAcount.currency (e.g.
	 *            USD) != commodityCurrency (e.g. EURO)
	 * @param amount
	 *            Money amount
	 * @param pricePerUnit
	 * @param settlementEvent
	 */
	public void placeSettlementSellingOffer(Currency commodityCurrency,
			Agent offeror, BankAccount offerorsBankAcount, double amount,
			double pricePerUnit,
			BankAccount commodityCurrencyOfferorsBankAcount,
			String commodityCurrencyOfferorsBankAcountPassword,
			ISettlementEvent settlementEvent) {
		if (amount > 0) {
			this.placeSellingOffer(commodityCurrency, offeror,
					offerorsBankAcount, amount, pricePerUnit,
					commodityCurrencyOfferorsBankAcount,
					commodityCurrencyOfferorsBankAcountPassword);
			if (settlementEvent != null)
				this.settlementEventListeners.put(offeror, settlementEvent);
		}
	}

	public void placeSettlementSellingOffer(Property property, Agent offeror,
			BankAccount offerorsBankAcount, double amount, double pricePerUnit,
			ISettlementEvent settlementEvent) {
		if (amount > 0) {
			this.placeSellingOffer(property, offeror, offerorsBankAcount,
					pricePerUnit);
			if (settlementEvent != null)
				this.settlementEventListeners.put(offeror, settlementEvent);
		}
	}

	public void removeAllSellingOffers(Agent offeror) {
		super.removeAllSellingOffers(offeror);
		this.settlementEventListeners.remove(offeror);
	}

	public Double[] buy(final GoodType goodType, final double maxAmount,
			final double maxTotalPrice, final double maxPricePerUnit,
			final Agent buyer, final BankAccount buyersBankAccount,
			final String buyersBankAccountPassword) {
		return this.buy(goodType, null, null, maxAmount, maxTotalPrice,
				maxPricePerUnit, buyer, buyersBankAccount,
				buyersBankAccountPassword, null);
	}

	public Double[] buy(final Currency commodityCurrency,
			final double maxAmount, final double maxTotalPrice,
			final double maxPricePerUnit, final Agent buyer,
			final BankAccount buyersBankAccount,
			final String buyersBankAccountPassword,
			final BankAccount buyersBankAccountForCommodityCurrency) {
		return this.buy(null, commodityCurrency, null, maxAmount,
				maxTotalPrice, maxPricePerUnit, buyer, buyersBankAccount,
				buyersBankAccountPassword,
				buyersBankAccountForCommodityCurrency);
	}

	public Double[] buy(final Class<? extends Property> propertyClass,
			final double maxAmount, final double maxTotalPrice,
			final double maxPricePerUnit, final Agent buyer,
			final BankAccount buyersBankAccount,
			final String buyersBankAccountPassword) {
		return this.buy(null, null, propertyClass, maxAmount, maxTotalPrice,
				maxPricePerUnit, buyer, buyersBankAccount,
				buyersBankAccountPassword, null);
	}

	protected Double[] buy(final GoodType goodType,
			final Currency commodityCurrency,
			final Class<? extends Property> propertyClass,
			final double maxAmount, final double maxTotalPrice,
			final double maxPricePerUnit, final Agent buyer,
			final BankAccount buyersBankAccount,
			final String buyersBankAccountPassword,
			final BankAccount buyersBankAccountForCommodityCurrency) {

		SortedMap<MarketOrder, Double> marketOffers = this
				.findBestFulfillmentSet(buyersBankAccount.getCurrency(),
						maxAmount, maxTotalPrice, maxPricePerUnit, goodType,
						commodityCurrency, propertyClass);

		Bank buyersBank = buyersBankAccount.getManagingBank();
		PropertyRegister register = PropertyRegister.getInstance();

		double moneySpentSum = 0;
		double amountSum = 0;
		Double[] priceAndAmount = new Double[2];

		for (Entry<MarketOrder, Double> entry : marketOffers.entrySet()) {
			MarketOrder marketOffer = entry.getKey();
			double amount = entry.getValue();

			// transfer money
			buyersBank.transferMoney(buyersBankAccount,
					marketOffer.getOfferorsBankAcount(),
					amount * marketOffer.getPricePerUnit(),
					buyersBankAccountPassword, "price for " + amount
							+ " units of " + marketOffer.getCommodity());

			// transfer ownership
			switch (marketOffer.getCommodityType()) {
			case GOODTYPE:
				register.transfer(marketOffer.getOfferor(), buyer,
						marketOffer.getGoodType(), amount);
				if (this.settlementEventListeners.containsKey(marketOffer
						.getOfferor())
						&& this.settlementEventListeners.get(marketOffer
								.getOfferor()) != null)
					this.settlementEventListeners.get(marketOffer.getOfferor())
							.onEvent(
									marketOffer.getGoodType(),
									amount,
									marketOffer.getPricePerUnit(),
									marketOffer.getOfferorsBankAcount()
											.getCurrency());
				// register market tick
				Log.market_onTick(marketOffer.getPricePerUnit(), marketOffer
						.getGoodType(), marketOffer.getOfferorsBankAcount()
						.getCurrency(), amount);
				break;
			case CURRENCY:
				Bank bank = marketOffer
						.getCommodityCurrencyOfferorsBankAccount()
						.getManagingBank();
				bank.transferMoney(
						marketOffer.getCommodityCurrencyOfferorsBankAccount(),
						buyersBankAccountForCommodityCurrency,
						amount,
						marketOffer
								.getCommodityCurrencyOfferorsBankAccountPassword(),
						"price for " + amount + " units of "
								+ marketOffer.getCommodity());
				if (this.settlementEventListeners.containsKey(marketOffer
						.getOfferor())
						&& this.settlementEventListeners.get(marketOffer
								.getOfferor()) != null)
					this.settlementEventListeners.get(marketOffer.getOfferor())
							.onEvent(
									marketOffer.getCommodityCurrency(),
									amount,
									marketOffer.getPricePerUnit(),
									marketOffer.getOfferorsBankAcount()
											.getCurrency());
				// register market tick
				Log.market_onTick(marketOffer.getPricePerUnit(), marketOffer
						.getCommodityCurrency(), marketOffer
						.getOfferorsBankAcount().getCurrency(), amount);
				break;
			case PROPERTY:
				register.transfer(marketOffer.getOfferor(), buyer,
						marketOffer.getProperty(), 1);
				if (this.settlementEventListeners.containsKey(marketOffer
						.getOfferor())
						&& this.settlementEventListeners.get(marketOffer
								.getOfferor()) != null)
					this.settlementEventListeners.get(marketOffer.getOfferor())
							.onEvent(
									marketOffer.getProperty(),
									marketOffer.getPricePerUnit(),
									marketOffer.getOfferorsBankAcount()
											.getCurrency());
				break;
			default:
				throw new RuntimeException("CommodityType unknown");
			}

			marketOffer.decrementAmount(amount);
			if (marketOffer.getAmount() <= 0)
				this.removeSellingOffer(marketOffer);

			moneySpentSum += amount * marketOffer.getPricePerUnit();
			amountSum += amount;
		}

		priceAndAmount[0] = moneySpentSum;
		priceAndAmount[1] = amountSum;

		if (priceAndAmount[1] > 0) {
			if (Log.isAgentSelectedByClient(buyer))
				Log.log(buyer,
						buyer
								+ " bought "
								+ MathUtil.round(priceAndAmount[1])
								+ " units of "
								+ marketOffers.firstKey().getCommodity()
								+ " for "
								+ Currency.round(priceAndAmount[0])
								+ " "
								+ buyersBankAccount.getCurrency()
										.getIso4217Code());
		}

		return priceAndAmount;
	}
}
