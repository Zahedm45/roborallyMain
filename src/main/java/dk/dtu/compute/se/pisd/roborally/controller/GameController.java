/*
 *  This file is part of the initial project provided for the
 *  course "Project in Software Development (02362)" held at
 *  DTU Compute at the Technical University of Denmark.
 *
 *  Copyright (C) 2019, 2020: Ekkart Kindler, ekki@dtu.dk
 *
 *  This software is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; version 2 of the License.
 *
 *  This project is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this project; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package dk.dtu.compute.se.pisd.roborally.controller;

import dk.dtu.compute.se.pisd.roborally.dal.IRepository;
import dk.dtu.compute.se.pisd.roborally.dal.RepositoryAccess;
import dk.dtu.compute.se.pisd.roborally.model.*;
import org.jetbrains.annotations.NotNull;

/**
 * ...
 *
 * @author Ekkart Kindler, ekki@dtu.dk
 *
 */
public class GameController {

    public Board board;

    public GameController(@NotNull Board board) {
        this.board = board;
    }

    /**
     * This is just some dummy controller operation to make a simple move to see something
     * happening on the board. This method should eventually be deleted!
     *
     * @param space the space to which the current player should move
     */
    public void moveCurrentPlayerToSpace(@NotNull Space space)  {
        // TODO Assignment V1: method should be implemented by the students:
        //   - the current player should be moved to the given space
        //     (if it is free()
        //   - and the current player should be set to the player
        //     following the current player
        //   - the counter of moves in the game should be increased by one
        //     if the player is moved

        Player current = board.getCurrentPlayer();
        if (space.getPlayer() == null) {
            current.setSpace(space);
            int number = board.getPlayerNumber(current);

            if (board.getPlayersNumber() <= (number + 1)) {
                board.setCurrentPlayer(board.getPlayer(0));
            } else {
                board.setCurrentPlayer(board.getPlayer(number + 1));
            }
            board.setStep(board.getStep() + 1);
        }
    }

    // XXX: V2
    public void startProgrammingPhase() {
        board.setPhase(Phase.PROGRAMMING);
        board.setCurrentPlayer(board.getPlayer(0));
        board.setStep(0);

        for (int i = 0; i < board.getPlayersNumber(); i++) {
            Player player = board.getPlayer(i);
            if (player != null) {
                for (int j = 0; j < Player.NO_REGISTERS; j++) {
                    CommandCardField field = player.getProgramField(j);
                    field.setCard(null);
                    field.setVisible(true);
                }
                for (int j = 0; j < Player.NO_CARDS; j++) {
                    CommandCardField field = player.getCardField(j);
                    field.setCard(generateRandomCommandCard());
                    field.setVisible(true);
                }
            }
        }
    }

    // XXX: V2
    private CommandCard generateRandomCommandCard() {
        Command[] commands = Command.values();
        int random = (int) (Math.random() * commands.length);
        return new CommandCard(commands[random]);
    }

    // XXX: V2
    public void finishProgrammingPhase() {
        makeProgramFieldsInvisible();
        makeProgramFieldsVisible(0);
        board.setPhase(Phase.ACTIVATION);
        board.setCurrentPlayer(board.getPlayer(0));
        board.setStep(0);
    }

    // XXX: V2
    private void makeProgramFieldsVisible(int register) {
        if (register >= 0 && register < Player.NO_REGISTERS) {
            for (int i = 0; i < board.getPlayersNumber(); i++) {
                Player player = board.getPlayer(i);
                CommandCardField field = player.getProgramField(register);
                field.setVisible(true);
            }
        }
    }

    // XXX: V2
    private void makeProgramFieldsInvisible() {
        for (int i = 0; i < board.getPlayersNumber(); i++) {
            Player player = board.getPlayer(i);
            for (int j = 0; j < Player.NO_REGISTERS; j++) {
                CommandCardField field = player.getProgramField(j);
                field.setVisible(false);
            }
        }
    }

    // XXX: V2
    public void executePrograms() {
        board.setStepMode(false);
        continuePrograms();

    }

    // XXX: V2
    public void executeStep() {
        board.setStepMode(true);
        continuePrograms();

    }

    // XXX: V2
    private void continuePrograms() {
        do {
            executeNextStep();
        } while (board.getPhase() == Phase.ACTIVATION && !board.isStepMode());
    }

    // XXX: V2
    private void executeNextStep() {
        //AppController appController;
        Player currentPlayer = board.getCurrentPlayer();
        if (board.getPhase() == Phase.ACTIVATION && currentPlayer != null) {
            int step = board.getStep();
            if (step >= 0 && step < Player.NO_REGISTERS) {
                CommandCard card = currentPlayer.getProgramField(step).getCard();
                if (card != null) {
                    Command command = card.command;
                    if (command.isInteractive()) {
                        board.setPhase(Phase.PLAYER_INTERACTION);
                        saveOrUpdateGame();
                        return;
                    }
                    executeCommand(currentPlayer, command);
                }
                int nextPlayerNumber = board.getPlayerNumber(currentPlayer) + 1;
                if (nextPlayerNumber < board.getPlayersNumber()) {
                    board.setCurrentPlayer(board.getPlayer(nextPlayerNumber));
                } else {
// action on the fields.

                    for (int i = 0; i < this.board.getPlayersNumber(); i++) {
                        for (FieldAction action : this.board.getPlayer(i).getSpace().getActions()) {
//                if (won)
//                    break;
                            action.doAction(this, this.board.getPlayer(i).getSpace());
                        }
                    }

                    step++;
                    if (step < Player.NO_REGISTERS) {
                        makeProgramFieldsVisible(step);
                        board.setStep(step);
                        board.setCurrentPlayer(board.getPlayer(0));
                    } else {
                        startProgrammingPhase();
                    }
                }
                saveOrUpdateGame();
            } else {
                // this should not happen
                assert false;
            }
        } else {
            // this should not happen
            assert false;
        }
    }


    /**
     * This method is in charge of executing the interactive command-cards
     * by bringing the program back to the activation phase.
     * And afterwords, the program continues once again until the interactive
     * command-cards occurs.
     *
     * @param option is the direction (left or right).
     * @author Zahed(s186517)
     */
    public void executeCommandOptionContinue(@NotNull Command option) {
        Player currentPlayer = board.getCurrentPlayer();
        if (currentPlayer != null &&
                board.getPhase() == Phase.PLAYER_INTERACTION &&
                option != null) {
            board.setPhase(Phase.ACTIVATION);
            executeCommand(currentPlayer, option);
            int nextPlayerNumber = board.getPlayerNumber(currentPlayer) + 1;
            if (nextPlayerNumber < board.getPlayersNumber()) {
                board.setCurrentPlayer(board.getPlayer(nextPlayerNumber));
            } else {
                int step = board.getStep() + 1;
                if (step < Player.NO_REGISTERS) {
                    makeProgramFieldsVisible(step);
                    board.setStep(step);
                    board.setCurrentPlayer(board.getPlayer(0));
                } else {
                    startProgrammingPhase();
                }
            }
            continuePrograms();
        }
    }





    // XXX: V2
    private void executeCommand(@NotNull Player player, Command command) {
        if (player != null && player.board == board && command != null) {
            // XXX This is a very simplistic way of dealing with some basic cards and
            //     their execution. This should eventually be done in a more elegant way
            //     (this concerns the way cards are modelled as well as the way they are executed).

            switch (command) {
                case FORWARD:
                    this.moveForward(player);
                    break;
                case RIGHT:
                    this.turnRight(player);
                    break;
                case LEFT:
                    this.turnLeft(player);
                    break;
                case FAST_FORWARD:
                    this.fastForward(player);
                    break;
                case U_TURN:
                    this.turnRight(player);
                    this.turnRight(player);
                    break;
                case MOVE_3:
                    this.fastForward(player);
                    this.moveForward(player);
                    break;
                case BACK_UP:
                    backUp(player);
                    break;

                default:
                    // DO NOTHING (for now)
            }
        }
    }

    /**
     * This method pushes other players that are in front of the current player.
     * In other words, if there any barriers for a player to move forward,
     * cause of other robots' stands, then the other players will be pushed by
     * the current player.
     * @param player current player
     * @param space
     * @param heading
     * @throws ImpossibleMoveException Throws exception if there any barriers
     * to move other then players, for instance walls.
     * @author Zahed(s186517)
     */

    public void moveToSpace(@NotNull Player player,
                            @NotNull Space space, @NotNull Heading heading) throws
            ImpossibleMoveException {
        Player other = space.getPlayer();
        // if there is a wall
        if (player.getSpace().getWalls().contains(heading)) {
            return;
        }


        if (other != null) {
            Space target = board.getNeighbour(space, heading);
            if (target != null) {
                moveToSpace(other, target, heading);
            } else {
                throw new ImpossibleMoveException(player, space, heading);
            }
        }
        player.setSpace(space);
        //space.runActions(this);
    }

    public void backUp(@NotNull Player player) {
        Heading heading = null;

        switch (player.getHeading()) {
            case SOUTH:
                heading = Heading.NORTH;
                break;
            case EAST:
                heading = Heading.WEST;
                break;
            case NORTH:
                heading = Heading.SOUTH;
                break;
            case WEST:
                heading = Heading.EAST;
                break;
        }
        Space target =
                player.getSpace().board.getNeighbour(player.getSpace(), heading);
        try {
            moveToSpace(player, target, heading);
        } catch (ImpossibleMoveException e) {
            e.printStackTrace();
        }


    }


    // TODO Assignment V2
    public void moveForward(@NotNull Player player) {
        Space current = player.getSpace();
        Heading heading = player.getHeading();


        if (current != null && player.board == current.board) {
            Space target = board.getNeighbour(current, player.getHeading());
            if (target != null) {
                try {
                    moveToSpace(player, target, heading);

                } catch (ImpossibleMoveException e) {// we don't do anything here  for now;// we just catch theexception so that// we do no pass it on to the caller// (which would be very bad style).}   }   }   }
                }
            }
        }
    }

    // TODO Assignment V2
    public void fastForward(@NotNull Player player) {
        moveForward(player);
        moveForward(player);

    }

    // TODO Assignment V2
    public void turnRight(@NotNull Player player) {
        if(player!=null&&player.board==board){
            player.setHeading(player.getHeading().next());
        }

    }

    // TODO Assignment V2
    public void turnLeft(@NotNull Player player) {
        if(player!=null&&player.board==board){
            player.setHeading(player.getHeading().prev());
        }
    }

    public boolean moveCards(@NotNull CommandCardField source, @NotNull CommandCardField target) {
        CommandCard sourceCard = source.getCard();
        CommandCard targetCard = target.getCard();
        if (sourceCard != null && targetCard == null) {
            target.setCard(sourceCard);
            source.setCard(null);
            return true;
        } else {
            return false;
        }
    }



    public void saveOrUpdateGame() {
        // XXX needs to be implemented eventually
        IRepository repository = RepositoryAccess.getRepository();
        Integer currentGameID = this.board.getGameId();
        // need to rewrite
        if (currentGameID == null && !repository.getGames().contains(currentGameID)) {
            repository.createGameInDB(this.board);
        } else {
            repository.updateGameInDB(this.board);
        }
    }

}




class ImpossibleMoveException extends Exception {

    private Player player;
    private Space space;
    private Heading heading;

    public ImpossibleMoveException(Player player, Space space, Heading heading) {
        super("Move impossible");
        this.player = player;
        this.space = space;
        this.heading = heading;
    }
}
