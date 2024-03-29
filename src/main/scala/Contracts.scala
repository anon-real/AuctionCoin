object Contracts {

  val buyBack: String =
    """{
      |  // we could pay less fee (e.g., 1e6) and add the rest to AC box to ensure it can create auction boxes
      |  val MaxMinerFee = MAX_MINER_FEE
      |
      |  val teamFee = TEAM_FEE // in percent
      |  val lpBox = if (INPUTS.size > 2) INPUTS(0) else CONTEXT.dataInputs(0)
      |  val acBox = INPUTS(1)
      |
      |  val successfulAuction = SELF.tokens.size == 0
      |
      |  // OUTPUTS(0) is LP box
      |  val outAc = OUTPUTS(1)
      |  val teamOut = OUTPUTS.getOrElse(2, OUTPUTS(0))
      |
      |  val NFT = acBox.tokens(0)._1
      |  val ac = acBox.tokens(1)
      |
      |  val rightAcBox = NFT == ACNFT
      |
      |  // AMM swap
      |  val fundInp = SELF.value - MaxMinerFee - (SELF.value * teamFee) / 100
      |  val yRes = lpBox.value
      |  val xRes = lpBox.tokens(2)._2
      |  val feeCoef = lpBox.R4[Int].get
      |  val willGet = (xRes.toBigInt * fundInp.toBigInt * feeCoef) / (yRes.toBigInt * 1000L + fundInp.toBigInt * feeCoef)
      |
      |  val rightOutAc = outAc.tokens(0)._1 == NFT && outAc.tokens(1)._2 >= ac._2 + willGet
      |  val rightLpBox = lpBox.tokens(0)._1 == CONFIG_LP
      |
      |  val rightTeamOut = teamOut.value >= (SELF.value * teamFee) / 100
      |  val rightTeamBox = teamOut.propositionBytes == TEAM_ADDRESS && rightTeamOut
      |
      |  val buyBack = successfulAuction && rightOutAc && rightLpBox && rightTeamBox && INPUTS.size == 3
      |
      |  // we return the tokens to AC box
      |  val failedAuction = {
      |    val toReturn = SELF.tokens.getOrElse(0, (Coll[Byte](), 0L))
      |    val rightTok = toReturn._1 == acBox.tokens(1)._1
      |    val addToAc = OUTPUTS(0).tokens(0)._1 == NFT && OUTPUTS(0).tokens(1)._2 >= ac._2 + toReturn._2 &&
      |                   OUTPUTS(0).value >= acBox.value + SELF.value - MaxMinerFee
      |    rightTok && addToAc && INPUTS.size == 2 && !successfulAuction && INPUTS.size == 2
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
      |  // R5: Coll[Coll[Long]] - Auction info: [numCoinsToAuction, period, coef, decrease_coef]
      |  val MaxMinerFee = MAX_MINER_FEE
      |  val auctionBoxInitialVal = AUC_BOX_INIT_VAL
      |
      |  val HOUR = 3600000L
      |  val curTime = CONTEXT.preHeader.timestamp
      |
      |  val lpBox = CONTEXT.dataInputs(0) // to get the price
      |
      |  val prevTime = SELF.R4[Coll[Long]].get(0) // last time we have started auctions
      |  val frequency = SELF.R4[Coll[Long]].get(1)
      |  val enoughTimePassed = curTime > prevTime
      |
      |  // list of auctions to start. for example we can start a batch of 5 auctions with different parameters every 3 days.
      |  val auctionInfo = SELF.R5[Coll[Coll[Long]]].get
      |  val NFT = SELF.tokens(0) // AC NFT
      |  val coin = SELF.tokens(1) // AC itself
      |  val coinId = coin._1
      |  val coinAm = coin._2
      |
      |  // check all auctions to be started correctly
      |  val allOkay = auctionInfo.indices.forall({(ind: Int) => {
      |    val auction = auctionInfo(ind)
      |    val numToAuction = auction(0)
      |    val period = auction(1)
      |    // (coef and decrease_coef are (1000 / actual coef))
      |    val coef = auction(2)
      |    val decrease_coef = auction(3)
      |
      |    val aucOut = OUTPUTS.getOrElse(ind + 1, INPUTS(0))
      |    val rightContract = blake2b256(aucOut.propositionBytes) == AuctionContractHash
      |    val aucToken = aucOut.tokens.getOrElse(0, (Coll[Byte](), 0L))
      |    val rightTokens = aucToken._1 == coinId && aucToken._2 == numToAuction
      |    val aucSt = curTime + (HOUR/2)
      |    val aucEnd = aucSt + period
      |    val lpErg = lpBox.value
      |    val lpAcAm = lpBox.tokens(2)._2
      |    val lpPrice = (lpErg / lpAcAm) * numToAuction
      |    val stPrice = (lpPrice * 1000) / coef
      |    val endPrice = (lpPrice * 1000) / decrease_coef
      |    val numDecrease = period / HOUR - 1
      |    val amountDecrease = (stPrice - endPrice) / numDecrease
      |
      |    val rightRegisters = blake2b256(aucOut.R4[Coll[Byte]].get) == BuyBackContractHash &&
      |                           aucOut.R5[(Long, Long)].get._1 >= aucSt - HOUR && aucOut.R5[(Long, Long)].get._1 <= aucSt + HOUR &&
      |                           aucOut.R5[(Long, Long)].get._2 >= aucEnd - HOUR && aucOut.R5[(Long, Long)].get._2 <= aucEnd + HOUR &&
      |                           aucOut.R6[Coll[Long]].get == Coll(stPrice, amountDecrease, HOUR) &&
      |                           aucOut.R7[Coll[Byte]].get == Coll[Byte]()
      |    rightContract && rightTokens && rightRegisters && aucOut.value == auctionBoxInitialVal
      |  }})
      |
      |  val total = auctionInfo.indices.map({(ind: Int) => auctionInfo(ind)(0)}).fold(0L, {(a: Long, b: Long) => a + b})
      |
      |  val correctNFT = OUTPUTS(0).tokens(0) == NFT
      |  val correctCoin = OUTPUTS(0).tokens(1)._1 == coinId && OUTPUTS(0).tokens(1)._2 >= coinAm - total
      |
      |  val rightRegisters = OUTPUTS(0).R4[Coll[Long]].get == Coll(prevTime + frequency, frequency) &&
      |                       OUTPUTS(0).R5[Coll[Coll[Long]]] == SELF.R5[Coll[Coll[Long]]]
      |
      |  // in case of failed auctions, we get back the tokens or in case of successful auctions we buy back the tokens
      |  val sameScript = OUTPUTS(0).propositionBytes == SELF.propositionBytes
      |  val allowAddingToken = {
      |    val selfOut = if (INPUTS.size == 2) OUTPUTS(0) else OUTPUTS(1)
      |    val okayTokens = selfOut.tokens(0) == NFT && selfOut.tokens(1)._1 == coinId && selfOut.tokens(1)._2 >= coinAm
      |    val keepRegisters = selfOut.R4[Coll[Long]] == SELF.R4[Coll[Long]] &&
      |                         selfOut.R5[Coll[Coll[Long]]] == SELF.R5[Coll[Coll[Long]]]
      |    val sameVal = selfOut.value >= SELF.value // allows adding ERGs to ensure auction creation
      |    okayTokens && keepRegisters && sameVal && selfOut.propositionBytes == SELF.propositionBytes
      |  }
      |
      |  val rightLpBox = lpBox.tokens(0)._1 == CONFIG_LP
      |  val minerFeeOkay = OUTPUTS(OUTPUTS.size - 1).value <= MaxMinerFee
      |  val startAuctions = rightLpBox && correctNFT && correctCoin && allOkay && rightRegisters &&
      |                        enoughTimePassed && OUTPUTS.size == auctionInfo.size + 2 && minerFeeOkay && INPUTS.size == 1
      |  sigmaProp((startAuctions && sameScript) || (allowAddingToken))
      |}""".stripMargin



}
