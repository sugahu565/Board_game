package game.domain

import game.domain.cards.Monster

class Player(val UID: Int, val name: String) {
    private val hand: MutableList<Card> = mutableListOf()
    private val discarded: MutableList<Card> = mutableListOf()
    private val played: MutableList<Card> = mutableListOf()

    fun addCard(card: Card) {
        hand.add(card)
    }

    fun removeCard(card: Card) {
        hand.remove(card)
    }

    fun playCard(card: Card) {
        hand.remove(card)
        played.add(card)
    }

    fun discardCard(card: Card) {
        hand.remove(card)
        discarded.add(card)
    }

    fun getCards(): List<Card> = hand.toList()

    fun getDiscarded(): List<Card> = discarded.toList()

    fun getPlayed(): List<Card> = played.toList()

    fun hasMonsterCard(): Boolean = hand.any { it is Monster }

    fun hasHideCard(): Boolean = hand.any { it is game.domain.cards.Hide }

    fun hasCard(card: Card): Int = hand.count { it == card }
}
