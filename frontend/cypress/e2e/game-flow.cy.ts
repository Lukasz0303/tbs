describe('Game Flow E2E', () => {
  beforeEach(() => {
    cy.visit('/');
  });

  it('should navigate to game options', () => {
    cy.visit('/game-options');
    cy.get('[data-cy="game-options"]', { timeout: 15000 }).should('be.visible');
  });

  it('should display game mode selection', () => {
    cy.visit('/game-options');
    cy.get('[data-cy="game-mode-card"]', { timeout: 15000 }).should('have.length.at.least', 1);
  });

  it('should navigate to leaderboard', () => {
    cy.visit('/leaderboard');
    cy.get('[data-cy="leaderboard"]', { timeout: 15000 }).should('be.visible');
  });
});

