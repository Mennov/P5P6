package bankieren;

import fontys.util.NumberDoesntExistException;
import fontys.observer.BasicPublisher;
import fontys.observer.RemotePropertyListener;
import fontys.observer.RemotePublisher;
import java.beans.PropertyChangeEvent;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Bank extends UnicastRemoteObject implements IBank
{

    /**
     *
     */
    private static final long serialVersionUID = -8728841131739353765L;
    private Map<Integer, IRekeningTbvBank> accounts;
    private Collection<IKlant> clients;
    private int nieuwReknr;
    private String name;

    private BasicPublisher publisher;

    public Bank(String name) throws RemoteException
    {
        accounts = new HashMap<>();
        clients = new ArrayList<>();
        nieuwReknr = 100000000;
        this.name = name;

        String[] publishString = new String[1];
        publishString[0] = "saldo";

        this.publisher = new BasicPublisher(publishString);

    }

    public synchronized int openRekening(String name, String city)
    {
        if (name.equals("") || city.equals(""))
        {
            return -1;
        }

        IKlant klant = getKlant(name, city);
        IRekeningTbvBank account = new Rekening(nieuwReknr, klant, Money.EURO);
        accounts.put(nieuwReknr, account);
        nieuwReknr++;
        return nieuwReknr - 1;
    }

    private IKlant getKlant(String name, String city)
    {
        for (IKlant k : clients)
        {
            if (k.getNaam().equals(name) && k.getPlaats().equals(city))
            {
                return k;
            }
        }
        IKlant klant = new Klant(name, city);
        clients.add(klant);
        return klant;
    }

    public IRekening getRekening(int nr)
    {
        return accounts.get(nr);
    }

    @Override
    public synchronized boolean maakOver(int source, int destination, Money money)
            throws NumberDoesntExistException, RemoteException
    {
        if (source == destination)
        {
            throw new RuntimeException(
                    "cannot transfer money to your own account");
        }
        if (!money.isPositive())
        {
            throw new RuntimeException("money must be positive");
        }

        IRekeningTbvBank source_account = (IRekeningTbvBank) getRekening(source);
        if (source_account == null)
        {
            throw new NumberDoesntExistException("account " + source
                    + " unknown at " + name);
        }

        Money negative = Money.difference(new Money(0, money.getCurrency()),
                money);
        boolean success = source_account.muteer(negative);
        if (!success)
        {
            return false;
        }

        IRekeningTbvBank dest_account = (IRekeningTbvBank) getRekening(destination);
        if (dest_account == null)
        {
            throw new NumberDoesntExistException("account " + destination
                    + " unknown at " + name);
        }
        success = dest_account.muteer(money);

        if (!success) // rollback
        {
            source_account.muteer(money);
        } else
        {
            publisher.inform(this, "saldo", null, null);
        }

        return success;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void addListener(RemotePropertyListener listener, String property) throws RemoteException
    {
        publisher.addListener(listener, property);
    }

    @Override
    public void removeListener(RemotePropertyListener listener, String property) throws RemoteException
    {
        publisher.removeListener(listener, property);

    }

}
