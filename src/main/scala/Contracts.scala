object Contracts {
  //  this contains ergo script contracts
  //  needed contracts
  //  1. main contract that:
  //    - contains the coins
  //    - accepts coins that have failed to be sold in auction
  //    - starts different kinds of auctions periodically

  //  2. sold contract that:
  //    - contains ERGs that have been earned from an auction
  //    - buys back the coins from LP (how to include randomness?)
  //    - sends the coins to the main contract (either failed auctions or bought back coins)


  //  here is the auction required registers
  //  - R4: seller ergo tree: Coll[Byte]
  //  - R5: start time, end time, (Long, Long)
  //  - R6 timed: min price, step, instantBuyAm: Coll[Long]
  //  - R6 fixed: min price, decline price, decline period
  //  - R7: currency ID, empty in case of ERG: Coll[Byte]
  //  - R8: bidder ergo tree: Coll[Byte]


  val buyBack: String =
    """{
      |  val MaxMinerFee = 10000000L
      |  val teamFee = 2 // in percent
      |  val lpBox = INPUTS(0)
      |  val acBox = INPUTS(1)
      |
      |  val successfulAuction = acBox.tokens.size == 0
      |
      |  val outAc = OUTPUTS(1)
      |  val teamOut = OUTPUTS(2)
      |
      |  val NFT = acBox.tokens(0)._1
      |  val ac = acBox.tokens(1)
      |
      |  val rightAcBox = NFT == ACNFT
      |
      |  // AMM swap
      |  val fundInp = SELF.value - MaxMinerFee - (SELF.value * teamFee) / 100
      |  val xRes = lpBox.value
      |  val yRes = acBox.tokens(2)._2
      |  val feeCoef = lpBox.R4[Int].get
      |  val willGet = (xRes * fundInp * feeCoef) / (yRes * 1000L + fundInp * feeCoef)
      |
      |  val rightOutAc = outAc.tokens(0)._1 == NFT && outAc.tokens(1)._2 >= ac._2 + willGet
      |  val rightLpBox = lpBox.tokens(2)._1 == CONFIG_LP
      |
      |  val rightTeamOut = teamOut.value >= (SELF.value * teamFee) / 100
      |  val rightTeamBox = teamOut.propositionBytes == TEAM_ADDRESS && rightTeamOut
      |
      |  val buyBack = successfulAuction && rightOutAc && rightLpBox && rightTeamBox && INPUTS.size == 3
      |
      |  val failedAuction = {
      |    val toReturn = SELF.tokens(0)
      |    val rightTok = toReturn._1 == acBox.tokens(1)._1
      |    val addToAc = OUTPUTS(0).tokens(0)._1 == NFT && OUTPUTS(0).tokens(1)._2 == ac._2 + toReturn._2 &&
      |                   OUTPUTS(0).value >= acBox.value + SELF.value - MaxMinerFee
      |    rightTok && addToAc && INPUTS.size == 2 && !successfulAuction
      |  }
      |
      |  sigmaProp((buyBack || failedAuction) && rightAcBox)
      |}""".stripMargin


  val main: String =
    """{
      |  // tokens: AC NFT, coins
      |
      |  // we start auctions (depending on Auction info below) each freq time (in timestamp)
      |  // R4: Coll[Long] - [prevTime, freq]
      |
      |  // we start decreasing price auction. The auction price will start from coef * LP price and will decrease to
      |  // LP price in the auction period. Coef could be set to 2 for example. This makes price discovery much easier!
      |  // R5: Coll[Coll[Long]] - Auction info: [numCoinsToAuction, period, coef]
      |
      |  val HOUR = 3600000L
      |  val HALFHOUR = 1800000L
      |
      |  val lpBox = CONTEXT.dataInputs(1) // to get the price
      |
      |  val prevTime = SELF.R4[Coll[Long]].get(0)
      |  val frequency = SELF.R4[Coll[Long]].get(1)
      |  val curTime = CONTEXT.preHeader.timestamp
      |  val enoughTimePassed = curTime > prevTime + frequency
      |
      |  val auctionInfo = SELF.R5[Coll[Coll[Long]]].get
      |  val NFT = SELF.tokens(0)
      |  val coin = SELF.tokens(1)
      |  val coinId = coin._1
      |  val coinAm = coin._2
      |
      |  val allOkay = auctionInfo.indices.forall({(ind: Int) => {
      |    val auction = auctionInfo(ind)
      |    val numToAuction = auction(0)
      |    val period = auction(1)
      |    val coef = auction(2) // (coef is 1000 / actual coef)
      |
      |    val aucOut = OUTPUTS(ind + 1)
      |    val rightContract = blake2b256(aucOut.propositionBytes) == AuctionContractHash
      |    val rightTokens = aucOut.tokens(1)._1 == coinId && aucOut.tokens(1)._2 == numToAuction
      |    val aucSt = curTime + HALFHOUR
      |    val aucEnd = aucSt + period
      |    val lpErg = lpBox.value
      |    val lpAcAm = lpBox.tokens(2)._2
      |    val lpPrice = (lpErg / lpAcAm) * numToAuction
      |    val stPrice = (lpPrice * 1000) / coef
      |    val numDecrease = period / HOUR
      |    val amountDecrease = (stPrice - lpPrice) / numDecrease
      |
      |    val rightRegisters = blake2b256(aucOut.R4[Coll[Byte]].get) == BuyBackContractHash &&
      |                           aucOut.R5[(Long, Long)].get == (aucSt, aucEnd) &&
      |                           aucOut.R6[Coll[Long]].get == Coll(stPrice, -amountDecrease, HOUR) &&
      |                           aucOut.R7[Coll[Byte]].get == Coll[Byte]()
      |    rightContract && rightTokens && rightRegisters
      |  }})
      |
      |  val total = auctionInfo.indices.map({(ind: Int) => {
      |    val auction = auctionInfo(ind)
      |    auction(1)
      |  }}).fold(0L, {(a: Long, b: Long) => a + b})
      |
      |  val correctNFT = OUTPUTS(0).tokens(0) == NFT
      |  val correctCoin = OUTPUTS(0).tokens(1)._1 == coinId && OUTPUTS(0).tokens(1)._2 == coinAm - total
      |
      |  val rightRegisters = OUTPUTS(0).R4[Coll[Long]].get == Coll(prevTime + frequency, frequency) &&
      |                       OUTPUTS(0).R5[Coll[Coll[Long]]] == SELF.R5[Coll[Coll[Long]]]
      |
      |  val allowAddingToken = {
      |    val selfOut = OUTPUTS(0)
      |    val okayTokens = selfOut.tokens(0) == NFT && selfOut.tokens(1)._1 == coinId && selfOut.tokens(1)._2 >= coinAm
      |    val keepRegisters = selfOut.R4[Coll[Long]].get == SELF.R4[Coll[Long]].get &&
      |                         selfOut.R5[Coll[Coll[Long]]].get == SELF.R5[Coll[Coll[Long]]].get
      |    val sameVal = selfOut.value == SELF.value
      |    val sameScript = selfOut.propositionBytes == SELF.propositionBytes
      |    okayTokens && keepRegisters && sameVal && sameScript
      |  }
      |
      |  val rightLpBox = lpBox.tokens(2)._1 == CONFIG_LP
      |  val startAuctions = rightLpBox && correctNFT && correctCoin && allOkay && rightRegisters && enoughTimePassed
      |  sigmaProp(startAuctions || allowAddingToken)
      |}""".stripMargin



}
